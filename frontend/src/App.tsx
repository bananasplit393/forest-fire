import { useEffect, useState } from "react";
import mqtt from "mqtt";
import { getFirefighters, getReadings, type Firefighter, type FireAlert, type Reading } from "./api";
import FireMap from "./FireMap";

const BROKER_URL = "ws://localhost:9001";
const MQTT_USERNAME = "forestfire";
const MQTT_PASSWORD = "mqtt-password";

export default function App() {
  const [readings, setReadings] = useState<Reading[]>([]);
  const [firefighters, setFirefighters] = useState<Firefighter[]>([]);
  const [liveAlerts, setLiveAlerts] = useState<Record<string, FireAlert>>({});
  const [liveFirefighters, setLiveFirefighters] = useState<Record<string, Firefighter>>({});

  // historik over målinger og brandmænd hvert 3. sekund (REST)
  useEffect(() => {
    const load = () => {
      getReadings().then(setReadings).catch(() => {});
      getFirefighters().then(setFirefighters).catch(() => {});
    };
    load();
    const timer = setInterval(load, 3000);
    return () => clearInterval(timer);
  }, []);

  // live kort: FireAlert og FirefighterLocation over MQTT (websockets)
  useEffect(() => {
    const c = mqtt.connect(BROKER_URL, { username: MQTT_USERNAME, password: MQTT_PASSWORD });
    c.on("connect", () => {
      c.subscribe("alerts/+", { qos: 1 });
      c.subscribe("firefighters/+/location", { qos: 0 });
    });
    c.on("message", (topic, payload) => {
      const parts = topic.split("/");
      if (parts[0] === "alerts") {
        const node = parts[1];
        setLiveAlerts((prev) => {
          const next = { ...prev };
          if (payload.length === 0) delete next[node];
          else next[node] = { ...JSON.parse(payload.toString()), receivedAt: Date.now() };
          return next;
        });
      } else if (parts[0] === "firefighters" && payload.length > 0) {
        const loc = JSON.parse(payload.toString());
        const id = loc.id ?? parts[1];
        setLiveFirefighters((prev) => ({
          ...prev,
          [id]: { id, lat: loc.lat, lon: loc.lon, receivedAt: new Date().toISOString() },
        }));
      }
    });
    return () => { c.end(); };
  }, []);

  const mapFirefighters = Object.values({
    ...Object.fromEntries(firefighters.map((f) => [f.id, f])),
    ...liveFirefighters,
  });
  const alerts = Object.values(liveAlerts).filter(
    (a) => a.receivedAt != null && Date.now() - a.receivedAt < 2 * 60 * 1000,
  );

  return (
    <main>
      <h1>Forest Fire</h1>
      <FireMap readings={readings} firefighters={mapFirefighters} alerts={alerts} />
      {alerts.length > 0 && (
        <ul className="alerts">
          {alerts.map((a) => (
            <li key={a.node}>
              Alarm {a.node}: {Math.round(a.fireProbability * 100)} %
              {a.lat != null && a.lon != null ? ` · ${a.lat.toFixed(4)}, ${a.lon.toFixed(4)}` : ""}
            </li>
          ))}
        </ul>
      )}
      <table>
        <thead>
          <tr>
            <th>Tid</th><th>Node</th><th>Temp</th><th>Fugt</th><th>PM10</th><th>PM2.5</th><th>Brand-sandsynlighed</th>
          </tr>
        </thead>
        <tbody>
          {readings.map((r) => (
            <tr key={r.id} className={r.isFire ? "fire" : ""}>
              <td>{new Date(r.timestamp).toLocaleString("da-DK")}</td>
              <td>{r.node}</td>
              <td>{r.temp}</td>
              <td>{r.humidity}</td>
              <td>{r.pm10}</td>
              <td>{r.pm25}</td>
              <td>{r.fireProbability != null ? `${Math.round(r.fireProbability * 100)} %` : ""}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}
