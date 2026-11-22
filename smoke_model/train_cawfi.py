# Træner skovbrand-modellen på CAWFI (California Wildfire Inventory) og eksporterer til ONNX
# Data: https://github.com/rohan-tan-bhowmik/CAWFI-Data (CC BY 4.0)
# Kør: python train_cawfi.py   (data/cawfi_prepared.csv bruges, hvis den findes)
# Byg datasættet igen: CAWFI_DIR="sti/til/CAWFI-Data/Original Data" python train_cawfi.py
import glob, os
from pathlib import Path
import numpy as np, pandas as pd, onnx, onnxruntime as ort
from onnx import helper, numpy_helper, TensorProto
from sklearn.neighbors import BallTree
from sklearn.model_selection import train_test_split
from sklearn.impute import SimpleImputer
from sklearn.pipeline import make_pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

HER = Path(__file__).parent
DATA = HER / "data" / "cawfi_prepared.csv"
MODEL = HER.parent / "api" / "src" / "ForestFire.Api" / "Model" / "model.onnx"
FEATS = ["temperature", "humidity", "pm25"]


# ---------- 1) Datasæt: én række pr. målestation pr. dag i Californien, 2012-2018 ----------
def load_epa(cawfi, folder):
    # EPA-filerne har ingen header. Vi tager kun Californien (state "06") og halvårsfilerne 2012-2018
    cols = {0: "state", 1: "county", 2: "site", 3: "param", 5: "lat", 6: "lon", 11: "date", 16: "value"}
    dfs = []
    for year in range(2012, 2019):
        for f in sorted(glob.glob(f"{cawfi}/{folder}/*_{year}_*To*.csv")):
            d = pd.read_csv(f, header=None, usecols=list(cols), dtype={0: str, 1: str, 2: str, 3: str}).rename(columns=cols)
            dfs.append(d[d.state == "06"])
    d = pd.concat(dfs)
    d["site_id"] = d.county + "-" + d.site
    return d


def build_dataset(cawfi):
    key = ["site_id", "date"]
    temp = load_epa(cawfi, "daily_TEMP")        # døgnmiddel i °F
    rh = load_epa(cawfi, "daily_dewpoint")      # 62201 = luftfugtighed %, 62103 = dugpunkt °F
    pm = load_epa(cawfi, "daily_pm25")          # døgnmiddel µg/m³

    df = temp.groupby(key).agg(lat=("lat", "first"), lon=("lon", "first"), temp_f=("value", "mean")).reset_index()
    df["temperature"] = (df.temp_f - 32) * 5 / 9
    df = df.merge(rh[rh.param == "62201"].groupby(key).value.mean().rename("humidity").reset_index(), on=key, how="left")
    df = df.merge(rh[rh.param == "62103"].groupby(key).value.mean().rename("dew_f").reset_index(), on=key, how="left")
    df = df.merge(pm.groupby(key).value.mean().rename("pm25").reset_index(), on=key, how="left")

    # Mangler målt fugt, regnes den ud fra temperatur og dugpunkt (Magnus-formlen)
    t, td = df.temperature, (df.dew_f - 32) * 5 / 9
    df["humidity"] = df.humidity.fillna(100 * np.exp(17.625 * td / (243.04 + td) - 17.625 * t / (243.04 + t)))
    df = df.dropna(subset=FEATS)

    # Label: brand = mindst 5 satellit-brande (FRP >= 7 MW, samme grænse som CAWFI's eget heatmap)
    # inden for 25 km af stationen samme dag
    df = df.reset_index(drop=True)
    df["fire"] = 0
    for date, g in df.groupby("date"):
        f = Path(cawfi) / "daily_wildfire" / f"{date}-locations.csv"
        if not f.exists():
            continue
        fires = pd.read_csv(f, header=None, names=["lon", "lat", "frp"]).drop_duplicates()
        fires = fires[fires.frp >= 7]
        if len(fires) == 0:
            continue
        tree = BallTree(np.radians(fires[["lat", "lon"]].values), metric="haversine")
        count = tree.query_radius(np.radians(g[["lat", "lon"]].values), r=25 / 6371.0, count_only=True)
        df.loc[g.index, "fire"] = (count >= 5).astype(int)
    # Kun dage, hvor der findes branddata (20. jan 2012 - 30. dec 2018)
    df = df[(df.date >= "2012-01-20") & (df.date <= "2018-12-30")]
    return df[["site_id", "date", "lat", "lon"] + FEATS + ["fire"]].round(3)


if os.environ.get("CAWFI_DIR") or not DATA.exists():
    df = build_dataset(os.environ.get("CAWFI_DIR", "CAWFI-Data/Original Data"))
    df.to_csv(DATA, index=False)
df = pd.read_csv(DATA)
print(len(df), "rækker,", df.site_id.nunique(), "stationer, andel brand:", round(df.fire.mean(), 4))
print("Korrelation med brand:", df[FEATS].corrwith(df.fire).round(3).to_dict())

# ---------- 2) Træning som i notebooks: median-imputer + RandomForest ----------
# Ingen StandardScaler: en RandomForest er ligeglad med skala, og scaleren gav afrundingsforskelle i ONNX
X, y = df[FEATS].values.astype(np.float32), df.fire.values
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, stratify=y, random_state=42)

# Parametre fundet med GridSearchCV (cv=5, scoring="roc_auc") på min_samples_leaf, max_features, max_depth
model = make_pipeline(SimpleImputer(strategy="median"),
                      RandomForestClassifier(n_estimators=150, max_depth=10, max_features=1, min_samples_leaf=50,
                                             class_weight="balanced", random_state=42, n_jobs=-1))
model.fit(X_train, y_train)

prob = model.predict_proba(X_test)[:, 1]
for t in [0.5, 0.8]:
    pred = prob >= t
    print(f"Grænse {t}: accuracy {accuracy_score(y_test, pred):.3f}, precision {precision_score(y_test, pred):.3f}, "
          f"recall {recall_score(y_test, pred):.3f}, F1 {f1_score(y_test, pred):.3f}")
print("ROC-AUC:", round(roc_auc_score(y_test, prob), 3))

# ---------- 3) ONNX: input [N,3] (temp, fugt, pm25), output fire_probability = sandsynlighed for brand ----------
onx = convert_sklearn(model, initial_types=[("input", FloatTensorType([None, 3]))],
                      options={id(model): {"zipmap": False}}, target_opset={"": 17, "ai.onnx.ml": 3})
g = onx.graph
probs = [o.name for o in g.output if "prob" in o.name][0]
# Tag kolonne 1 (brand) ud af [N,2] og gør den til det eneste output
g.initializer.extend([numpy_helper.from_array(np.array([1], np.int64), "c1"),
                      numpy_helper.from_array(np.array([2], np.int64), "c2")])
g.node.append(helper.make_node("Slice", [probs, "c1", "c2", "c1"], ["fire_probability"]))
del g.output[:]
g.output.append(helper.make_tensor_value_info("fire_probability", TensorProto.FLOAT, [None, 1]))
onx.ir_version = 8   # virker også med ældre ONNX Runtime
onnx.checker.check_model(onx)
onnx.save(onx, MODEL)

# ---------- 4) Tjek modellen med onnxruntime ----------
sess = ort.InferenceSession(str(MODEL))
print("ONNX vs sklearn, max forskel:", np.abs(sess.run(None, {"input": X_test})[0].ravel() - prob).max())
for navn, x in [("normalt skovvejr", [17, 65, 10]), ("brand", [55, 10, 300]), ("ingen data", [np.nan] * 3)]:
    print(navn, x, "->", round(float(sess.run(None, {"input": np.array([x], np.float32)})[0][0, 0]), 3))
