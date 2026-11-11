# Refit af den finetunede RandomForest fra SmokeDetectionLog.ipynb og eksport til ONNX
import numpy as np, pandas as pd, onnx, onnxruntime as ort
from onnx import helper, TensorProto, numpy_helper
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.pipeline import make_pipeline, Pipeline
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

FEATS = ["Temperature[C]", "Humidity[%]", "PM2.5"]
df = pd.read_csv("/workspace/smoke_o4/data/smoke_detection_iot.csv")
df = df.drop(columns=["Unnamed: 0", "UTC", "CNT"])
df_raw = df.copy()                                   # rå PM2.5 til ONNX-test
df['PM2.5'] = np.log1p(df['PM2.5'])
tr, te = train_test_split(df, test_size=0.2, stratify=df["Fire Alarm"], random_state=42)
tr_raw, te_raw = df_raw.loc[tr.index], df_raw.loc[te.index]
attributes = FEATS + ["Fire Alarm"]
train_set, test_set = tr[attributes].copy(), te[attributes].copy()
X_train = train_set.dropna(subset=["Fire Alarm"]).drop("Fire Alarm", axis=1)
y_train = train_set.dropna(subset=["Fire Alarm"])["Fire Alarm"].copy()
X_test = test_set.drop("Fire Alarm", axis=1); y_test = test_set["Fire Alarm"].copy()

# --- 1) Notebookens præcise pipeline + grid (for at finde/bekræfte best_params_) ---
# Kør med RUN_GRID=1 for at gentage grid search (ca. 5 min). Resultat (bekræftet):
# {'max_depth': None, 'max_features': 2, 'min_samples_leaf': 1, 'n_estimators': 150}, test RMSE 0.09381
import os
preprocessing = ColumnTransformer([
    ("num", make_pipeline(SimpleImputer(strategy="median"), StandardScaler()), FEATS),
    ("cat", make_pipeline(SimpleImputer(strategy="most_frequent"), OneHotEncoder(handle_unknown="ignore")), []),
])
full_pipeline = Pipeline([("preprocessing", preprocessing), ("random_forest", RandomForestRegressor(random_state=42))])
if os.environ.get("RUN_GRID"):
    param_grid = [{'random_forest__n_estimators': [30, 100, 150], 'random_forest__max_features': [1, 2, 3],
                   'random_forest__max_depth': [None, 10, 20], 'random_forest__min_samples_leaf': [1, 2, 4]}]
    gs = GridSearchCV(full_pipeline, param_grid, cv=10, scoring='neg_root_mean_squared_error', n_jobs=-1)
    gs.fit(X_train, y_train)
    best_params = gs.best_params_
else:
    best_params = {'random_forest__max_depth': None, 'random_forest__max_features': 2,
                   'random_forest__min_samples_leaf': 1, 'random_forest__n_estimators': 150}
print("best_params_:", best_params)
nb_model = full_pipeline.set_params(**best_params).fit(X_train, y_train)
nb_pred = nb_model.predict(X_test)
print("Notebook-pipeline test RMSE:", np.sqrt(mean_squared_error(y_test, nb_pred)))

# --- 2) Samme model på numpy-input [temp, humidity, log1p(pm25)] (uden tom cat-gren) ---
bp = {k.split("__")[1]: v for k, v in best_params.items()}
pipe = Pipeline([("imputer", SimpleImputer(strategy="median")), ("scaler", StandardScaler()),
                 ("random_forest", RandomForestRegressor(random_state=42, **bp))])
pipe.fit(X_train.values, y_train.values)
sk_pred = pipe.predict(X_test.values)
print("Numpy-pipeline test RMSE:", np.sqrt(mean_squared_error(y_test, sk_pred)),
      "| max diff vs notebook-pipeline:", np.abs(sk_pred - nb_pred).max())

# --- 3) ONNX: forbehandling (imputer, log1p, scaler) i double præcision som sklearn,
#            derefter cast til float og skl2onnx' TreeEnsembleRegressor for selve skoven ---
imp, sc, rf = pipe.named_steps["imputer"], pipe.named_steps["scaler"], pipe.named_steps["random_forest"]
rf_onx = convert_sklearn(rf, initial_types=[("x_scaled", FloatTensorType([None, 3]))],
                         target_opset={"": 17, "ai.onnx.ml": 3})
g = rf_onx.graph
out_name = g.output[0].name
I = lambda arr, name: numpy_helper.from_array(arr, name)
# Imputer-median gælder log1p-transformeret PM2.5 (som i notebooken), så den anvendes efter log1p
inits = [I(np.array([0], np.int64), "c0"), I(np.array([2], np.int64), "c2"), I(np.array([3], np.int64), "c3"),
         I(np.array([1], np.int64), "ax1"), I(np.array(1.0, np.float64), "one_d"),
         I(imp.statistics_.reshape(1, 3).astype(np.float64), "median_d"),
         I(sc.mean_.reshape(1, 3).astype(np.float64), "mean_d"), I(sc.scale_.reshape(1, 3).astype(np.float64), "scale_d")]
pre = [
    helper.make_node("Cast", ["input"], ["x_d"], to=TensorProto.DOUBLE, name="to_double"),
    helper.make_node("Slice", ["x_d", "c0", "c2", "ax1"], ["x_th"], name="slice_temp_hum"),
    helper.make_node("Slice", ["x_d", "c2", "c3", "ax1"], ["x_pm25"], name="slice_pm25"),
    helper.make_node("Add", ["x_pm25", "one_d"], ["x_pm25_p1"], name="pm25_plus1"),
    helper.make_node("Log", ["x_pm25_p1"], ["x_pm25_log"], name="pm25_log"),   # log1p(PM2.5)
    helper.make_node("Concat", ["x_th", "x_pm25_log"], ["x_log"], axis=1, name="concat_features"),
    helper.make_node("IsNaN", ["x_log"], ["x_nan"], name="isnan"),
    helper.make_node("Where", ["x_nan", "median_d", "x_log"], ["x_imp"], name="impute_median"),
    helper.make_node("Sub", ["x_imp", "mean_d"], ["x_c"], name="scaler_sub"),
    helper.make_node("Div", ["x_c", "scale_d"], ["x_s"], name="scaler_div"),
    helper.make_node("Cast", ["x_s"], ["x_scaled"], to=TensorProto.FLOAT, name="to_float"),
]
graph = helper.make_graph(pre + list(g.node), "forest_fire_rf",
    inputs=[helper.make_tensor_value_info("input", TensorProto.FLOAT, [None, 3])],
    outputs=[helper.make_tensor_value_info("fire_probability", TensorProto.FLOAT, [None, 1])],
    initializer=list(g.initializer) + inits)
for n in graph.node:
    n.output[:] = ["fire_probability" if o == out_name else o for o in n.output]
model = helper.make_model(graph, opset_imports=list(rf_onx.opset_import), producer_name="skl2onnx+log1p")
model.ir_version = 8   # kompatibel med ældre ONNX Runtime 1.x
onnx.checker.check_model(model)
onnx.save(model, "/workspace/backend_model/model.onnx")

# --- 4) Verifikation med onnxruntime på RÅ input ---
sess = ort.InferenceSession("/workspace/backend_model/model.onnx")
print("inputs:", [(i.name, i.shape, i.type) for i in sess.get_inputs()])
print("outputs:", [(o.name, o.shape, o.type) for o in sess.get_outputs()])
Xraw = te_raw[FEATS].values.astype(np.float32)
onx = sess.run(None, {"input": Xraw})[0].ravel()
print("ONNX vs sklearn: max abs diff (første 1000):", np.abs(onx[:1000] - sk_pred[:1000]).max(),
      "| alle:", np.abs(onx - sk_pred).max(), "| antal >0.01:", int((np.abs(onx - sk_pred) > 0.01).sum()))
print("ONNX test RMSE:", np.sqrt(mean_squared_error(y_test, onx)))
# Reference: sklearn på float32-rundede rå input (det C# sender) -> isolerer float-afrunding af input
Xr32 = Xraw.astype(np.float64); Xr32[:, 2] = np.log1p(Xr32[:, 2])
sk32 = pipe.predict(Xr32)
print("ONNX vs sklearn(float32-input): max abs diff:", np.abs(onx - sk32).max())
print("Accuracy @0.5 (ONNX):", ((onx >= 0.5).astype(int) == y_test.values).mean())
fire = te_raw[te_raw["Fire Alarm"] == 1].iloc[0]; nofire = te_raw[te_raw["Fire Alarm"] == 0].iloc[0]
for name, r in [("fire", fire), ("no fire", nofire)]:
    x = np.array([[r["Temperature[C]"], r["Humidity[%]"], r["PM2.5"]]], np.float32)
    print(name, "row", x.tolist(), "true", int(r["Fire Alarm"]), "-> onnx", sess.run(None, {"input": x})[0].tolist())
