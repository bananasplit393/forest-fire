import { useEffect, useState } from "react";
import { getFirefighters, getReadings, type Firefighter, type FireAlert, type Reading } from "./api";
import FireMap from "./FireMap";

export default function App() {
  const [readings, setReadings] = useState<Reading[]>([]);
  const [firefighters, setFirefighters] = useState<Firefighter[]>([]);

  // alt hentes fra backenden over HTTP hvert 3. sekund
  useEffect(() => {
    const load = () => {
      getReadings().then(setReadings).catch(() => {});
      getFirefighters().then(setFirefighters).catch(() => {});
    };
    load();
    const timer = setInterval(load, 3000);
    return () => clearInterval(timer);
  }, []);

  // alarm for en node, når dens nyeste måling er brand
  const latest = new Map<string, Reading>();
  for (const r of readings) if (r.node && !latest.has(r.node)) latest.set(r.node, r);
  const alerts: FireAlert[] = [...latest.values()]
    .filter((r) => r.isFire)
    .map((r) => ({ node: r.node!, lat: r.lat, lon: r.lon, fireProbability: r.fireProbability ?? 0 }));
  return (
    <main>
      <h1>Forest Fire</h1>
      <FireMap readings={readings} firefighters={firefighters} alerts={alerts} />
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
