# Forest Fire – API

```
Mosquitto ──SensorData/Location──▶ ForestFire.Api ──▶ risikomodel (model.onnx) ──▶ api-db (PostgreSQL)
                                       └──▶ FireAlert (alerts/{nodeId}, retained) til Mosquitto
Dashboard ──GET /readings, GET /firefighters──▶ ForestFire.Api
```

Samme opsætning som CoreService i 2026E74: Controllers → Services → EF Core → PostgreSQL.
API'et abonnerer selv direkte på MQTT (`MqttService` og `MqttFireAlertSender` under `src/ForestFire.Api/Mqtt`) og publicerer `FireAlert` ved brand. CORS tillader alle origins, så frontenden kan kalde det.

| Mappe | Indhold |
|---|---|
| `src/ForestFire.Api` | C#-koden (ASP.NET Core 9). |
| `src/ForestFire.Api/Model` | Læg `model.onnx` her. Kopieres med ud ved build. |
| `deploy/api/` | `Dockerfile` og `Dockerfile.dockerignore`. |
| `docker-compose.yml` | Hele stakken lokalt: API, database, Mosquitto, coap-serveren og frontenden. |

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
docker compose up --build
```

- API: http://localhost:8081
- Dashboard: http://localhost:3000
- MQTT (Mosquitto): `localhost:1883` (websockets `9001`), login `forestfire` / `mqtt-password`
- CoAP: `udp://localhost:5683`
- Databasen: `localhost:5434` (5433 er bachelor-databasen)

## Kør API'et direkte (debugging i Rider/VS)

```bash
docker compose up api-db mosquitto
dotnet run --project src/ForestFire.Api   # http://localhost:5200
```

`appsettings.Development.json` peger allerede på databasen og Mosquitto på localhost.

## Risikomodellen

Modellen trænes i Python og eksporteres med `skl2onnx`. Læg filen i `src/ForestFire.Api/Model/model.onnx` (eller sæt `Model__Path`).

Input skal være én float-tensor med shape `[1, 3]` i rækkefølgen `[temp, humidity, pm25]` (se `RiskModelService`). Mangler en værdi, sendes `NaN`, og modellen udfylder selv med medianen.

## Databasemigrationer

Migrationerne ligger i `src/ForestFire.Api/Migrations` og køres automatisk ved opstart, når `Database:MigrateOnStartup` er `true`. Ny migration efter en ændring i modellen:

```bash
dotnet tool install --global dotnet-ef
cd src/ForestFire.Api
dotnet ef migrations add <Navn>
```
