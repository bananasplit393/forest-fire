# Forest Fire – MQTT

> **Bemærk:** MQTT-håndteringen (`MqttService` og `MqttFireAlertSender`) er flyttet ind i `api/src/ForestFire.Api`, så backenden selv abonnerer direkte på Mosquitto (som i C2-diagrammet). Denne mappe beholdes af historiske årsager, men startes ikke længere i `docker-compose.yml` (`mosquitto.conf` bruges stadig af brokeren).

MQTT-delen af systemet: Mosquitto-brokerens config og `ForestFire.Mqtt`, servicen der står mellem brokeren og API'et.
Bygget ligesom Edger i 2026E74: en worker (ikke en webapp) med én `MqttService`, der både lytter (`BackgroundService`) og publicerer (`IMqttPublisher`).

```
Sensor nodes ──CoAP──▶ coap-server ──sensors/{nodeId}/data──▶ Mosquitto ──▶ ForestFire.Mqtt ──POST /readings──▶ ForestFire.Api
                                                                  ▲               │  ◀── måling med isFire ──┘
                                                                  └─ alerts/{nodeId} (retained), hvis isFire
Firefighter app (websockets :9001) ◀── alerts/+
Firefighter app ──firefighters/{id}/location──▶ Mosquitto ──▶ ForestFire.Mqtt ──PUT /firefighters/{id}──▶ ForestFire.Api
```

## Hvad ligger her

```
mqtt/
  mosquitto.conf              brokerens config (login kræves, websockets på 9001 til appen), bruges af api/docker-compose.yml
  ForestFire.Mqtt.csproj
  Program.cs                  DI, samme opsætning som Edger
  appsettings.json            topics, broker og API-adresse
  appsettings.Development.json  til `dotnet run` på din pc (localhost, dev-login)
  Dockerfile
  Interfaces/
    IMqttPublisher.cs         publish (med retain)
    IFireAlertSender.cs
    IForestFireApi.cs         HTTP-kaldene til API'et
  Models/                     SensorData, Reading (svaret fra API'et), FirefighterLocation
  Mqtt/
    MqttService.cs            forbinder, lytter, router beskederne, og er IMqttPublisher
    MqttFireAlertSender.cs    bygger FireAlert og publicerer den på alerts/{nodeId}
  Services/
    ForestFireApiClient.cs    POST /readings og PUT /firefighters/{id}
```

## Topics

| Topic | Publiceres af | Lyttes på af | QoS | Retained |
|---|---|---|---|---|
| SensorData `sensors/{nodeId}/data` | coap-server | ForestFire.Mqtt | 1 | nej |
| FireAlert `alerts/{nodeId}` | ForestFire.Mqtt | Firefighter app (`alerts/+`) | 1 | ja |
| FirefighterLocation `firefighters/{id}/location` | Firefighter app | ForestFire.Mqtt | 0 | nej |

```jsonc
// SensorData
{ "node": "node-1", "ts": 1794340800, "temp": 20.1, "humidity": 65, "pm10": 12.3, "pm25": 7.4, "lat": 55.67, "lon": 12.56 }

// FireAlert = SensorData + alarm. alertId er målingens id i API'et, så dubletter kan kendes
{ "alertId": "…", "node": "node-1", "ts": 1794340800, "temp": 48.2, "humidity": 20, "pm10": 310, "pm25": 250,
  "lat": 55.67, "lon": 12.56, "fireProbability": 0.97, "alarm": true }

// FirefighterLocation
{ "id": "ff-7", "lat": 55.66, "lon": 12.55 }
```

## Hvad servicen gør

- **SensorData**: node-id tages fra topic'et. Målingen sendes til `POST /readings`, hvor API'et kører modellen og gemmer den. Svarer API'et `isFire: true`, publiceres en FireAlert på `alerts/{nodeId}` (retained, så en app der forbinder senere også får den).
  Beskeden ackes først, når API'et har svaret. Er API'et nede, prøves den igen. Ugyldig JSON og 400 fra API'et smides væk.
- **FirefighterLocation**: sendes videre til `PUT /firefighters/{id}`, så dashboardet kan hente positionerne med `GET /firefighters`.
- Fast client-id og `CleanSession = false`, så Mosquitto gemmer SensorData (QoS 1), mens servicen er nede.

## Config

`config["Section:Key"]` som i Edger. Alt kan overskrives med miljøvariabler, `:` bliver `__` (fx `Mqtt__Host=localhost`).

| key | hvad | default |
|---|---|---|
| `Mqtt:Host` / `Mqtt:Port` | brokeren | `mosquitto` / `1883` |
| `Mqtt:ClientId` | client id | `forestfire-mqtt` |
| `Mqtt:Username` / `Mqtt:Password` | bruges kun hvis username ikke er tom | tom |
| `Mqtt:SensorDataTopic` | | `sensors/+/data` |
| `Mqtt:FirefighterLocationTopic` | | `firefighters/+/location` |
| `Mqtt:FireAlertTopic` | `{0}` er node-id | `alerts/{0}` |
| `Api:BaseUrl` | ForestFire.Api | `http://api:8080` |

## Kør den

Hele stakken (broker, API, database og denne service):

```bash
cd api
docker compose up --build
```

Direkte på din pc (debugging i Rider/VS):

```bash
cd api
docker compose up api-db mosquitto
dotnet run --project src/ForestFire.Api    # http://localhost:5200
dotnet run --project ../mqtt               # lytter på localhost:1883
```

## Test med mosquitto_pub/sub

```bash
cd api
# lyt på alle alarmer (retained alarmer kommer med det samme)
docker compose exec mosquitto mosquitto_sub -u forestfire -P mqtt-password -t "alerts/#" -v

# send en måling som coap-serveren ville
docker compose exec mosquitto mosquitto_pub -u forestfire -P mqtt-password -q 1 -t "sensors/node-1/data" \
  -m '{"node":"node-1","ts":1794340800,"temp":20.1,"humidity":65,"pm10":12.3,"pm25":7.4,"lat":55.67,"lon":12.56}'

# send en brandmands position og se den i GET /firefighters
docker compose exec mosquitto mosquitto_pub -u forestfire -P mqtt-password -t "firefighters/ff-7/location" \
  -m '{"id":"ff-7","lat":55.66,"lon":12.55}'

# fjern en retained alarm igen (tom besked med retain)
docker compose exec mosquitto mosquitto_pub -u forestfire -P mqtt-password -t "alerts/node-1" -r -n
```
