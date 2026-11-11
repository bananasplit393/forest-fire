# Forest Fire â€“ API

```
Producer â”€â”€CoAPâ”€â”€â–¶ coap-server â”€â”€MQTTâ”€â”€â–¶ forest-fire/readings
                                   
POST /readings â”€â”€â–¶ ForestFire.Api â”€â”€â–¶ risikomodel (model.onnx) â”€â”€â–¶ api-db (PostgreSQL)
```

Samme opsÃ¦tning som bachelor-backenden (CoreService): Controllers â†’ Services â†’ EF Core â†’ PostgreSQL.

| Mappe | Indhold |
|---|---|
| `src/ForestFire.Api` | C#-koden (ASP.NET Core 9). |
| `src/ForestFire.Api/Model` | LÃ¦g `model.onnx` her. Kopieres med ud ved build. |
| `deploy/api/` | `Dockerfile` og `Dockerfile.dockerignore`. |
| `docker-compose.yml` | API + database lokalt. |

## Endpoints

Body er samme JSON som producerne sender (`producers/interface/sensor.ts`):

```json
{ "ts": 1794340800, "temp": 20.1, "humidity": 65, "pm10": 12.3, "pm25": 7.4, "lat": 55.67, "lon": 12.56 }
```

| Metode | Sti | Svar |
|---|---|---|
| `POST` | `/readings` | Gemmer mÃ¥lingen, kÃ¸rer modellen og giver den gemte mÃ¥ling med `fireProbability` og `isFire`. `ts` skal med, resten er valgfri. |
| `GET` | `/readings?limit=100` | De nyeste mÃ¥linger, nyeste fÃ¸rst (limit 1â€“1000). |
| `GET` | `/readings/{id}` | Ã‰n mÃ¥ling, 404 hvis den ikke findes. |
| `DELETE` | `/readings/{id}` | 204, eller 404 hvis den ikke findes. |

## KÃ¸r lokalt med Docker

```bash
cd api
docker compose up --build
```

- API: http://localhost:8081
- Databasen: `localhost:5434` (5433 er bachelor-databasen)

## KÃ¸r API'et direkte (debugging i Rider/VS)

Start kun databasen i Docker og kÃ¸r API'et med `dotnet run`:

```bash
docker compose up api-db
dotnet run --project src/ForestFire.Api   # http://localhost:5200
```

`appsettings.Development.json` peger allerede pÃ¥ databasen. `ForestFire.Api.http` kan kÃ¸res direkte fra IDE'en.

## Risikomodellen

Modellen trÃ¦nes i Python og eksporteres med `skl2onnx`. LÃ¦g filen i `src/ForestFire.Api/Model/model.onnx` (eller sÃ¦t `Model__Path`).
Mangler filen, starter API'et stadig, og mÃ¥lingerne gemmes med `fireProbability` og `isFire` som `null`.

Input skal vÃ¦re Ã©n float-tensor med shape `[1, 4]` i rÃ¦kkefÃ¸lgen `[temp, humidity, pm10, pm25]`.
Output er det skl2onnx giver for en klassifikator: `label` (0/1) og `probabilities`. Mangler en af de fire mÃ¥linger, kÃ¸res modellen ikke.

## Databasemigrationer

Migrationerne ligger i `src/ForestFire.Api/Migrations` og kÃ¸res automatisk ved opstart, nÃ¥r `Database:MigrateOnStartup` er `true`. Ny migration efter en Ã¦ndring i modellen:

```bash
dotnet tool install --global dotnet-ef
cd src/ForestFire.Api
dotnet ef migrations add <Navn>
```
