# Forest Fire – API

```
ForestFire.Mqtt (../mqtt) ──POST /readings──▶ ForestFire.Api ──▶ risikomodel (model.onnx) ──▶ api-db (PostgreSQL)
Dashboard ──GET /readings, GET /firefighters──▶ ForestFire.Api
```

Samme opsætning som CoreService i 2026E74: Controllers → Services → EF Core → PostgreSQL.
API'et har ingen MQTT-kode. Al MQTT ligger i `../mqtt` (se README'en der).

| Mappe | Indhold |
|---|---|
| `src/ForestFire.Api` | C#-koden (ASP.NET Core 9). |
| `src/ForestFire.Api/Model` | Læg `model.onnx` her. Kopieres med ud ved build. |
| `deploy/api/` | `Dockerfile` og `Dockerfile.dockerignore`. |
| `docker-compose.yml` | Hele stakken lokalt: API, database, Mosquitto og mqtt-servicen. |

## Endpoints

| Metode | Sti | Svar |
|---|---|---|
| `POST` | `/readings` | Kaldes af mqtt-servicen med SensorData (body nedenfor). Gemmer målingen, kører modellen og giver den gemte måling med `fireProbability` og `isFire`. `ts` skal med, resten er valgfri. |
| `GET` | `/readings?limit=100` | De nyeste målinger, nyeste først (limit 1–1000). |
| `GET` | `/readings/{id}` | Én måling, 404 hvis den ikke findes. |
| `DELETE` | `/readings/{id}` | 204, eller 404 hvis den ikke findes. |
| `PUT` | `/firefighters/{id}` | Kaldes af mqtt-servicen med `{ "lat": …, "lon": … }`. 204. |
| `GET` | `/firefighters` | Den seneste position for hver brandmand (holdes kun i hukommelsen). |

```json
{ "node": "node-1", "ts": 1794340800, "temp": 20.1, "humidity": 65, "pm10": 12.3, "pm25": 7.4, "lat": 55.67, "lon": 12.56 }
```

## Kør lokalt med Docker

```bash
cd api
cp .env.example .env
docker compose up --build
```

- API: http://localhost:8081
- MQTT (Mosquitto): `localhost:1883`, login fra `.env`
- Databasen: `localhost:5434` (5433 er bachelor-databasen)

## Kør API'et direkte (debugging i Rider/VS)

```bash
docker compose up api-db mosquitto
dotnet run --project src/ForestFire.Api   # http://localhost:5200
```

`appsettings.Development.json` peger allerede på databasen. Se `../mqtt/README.md` for at køre mqtt-servicen ved siden af.

## Risikomodellen

Modellen trænes i Python og eksporteres med `skl2onnx`. Læg filen i `src/ForestFire.Api/Model/model.onnx` (eller sæt `Model__Path`).

Input skal være én float-tensor med shape `[1, 3]` i rækkefølgen `[temp, humidity, pm25]` (se `RiskModelService`).

## Databasemigrationer

Migrationerne ligger i `src/ForestFire.Api/Migrations` og køres automatisk ved opstart, når `Database:MigrateOnStartup` er `true`. Ny migration efter en ændring i modellen:

```bash
dotnet tool install --global dotnet-ef
cd src/ForestFire.Api
dotnet ef migrations add <Navn>
```
