import { CircleMarker, MapContainer, Popup, TileLayer } from "react-leaflet";
import type { FireAlert, Firefighter, Reading } from "./api";

interface Props {
  readings: Reading[];
  firefighters: Firefighter[];
  alerts: FireAlert[];
}

export default function FireMap({ readings, firefighters, alerts }: Props) {
  // den nyeste måling pr. node (readings er nyeste først). Live-alarmer vises for sig
  const alertNodes = new Set(alerts.map((a) => a.node));
  const latest = new Map<string, Reading>();
  for (const r of readings) {
    const node = r.node ?? "";
    if (r.lat != null && r.lon != null && !latest.has(node) && !alertNodes.has(node)) latest.set(node, r);
  }

  return (
    <MapContainer center={[56.0, 10.5]} zoom={7} className="map">
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="&copy; OpenStreetMap"
      />
      {[...latest.values()].map((r) => (
        <CircleMarker key={r.id} center={[r.lat!, r.lon!]} radius={10}
          pathOptions={{ color: r.isFire ? "red" : "green" }}>
          <Popup>{r.node}: {Math.round((r.fireProbability ?? 0) * 100)} %</Popup>
        </CircleMarker>
      ))}
      {alerts.filter((a) => a.lat != null && a.lon != null).map((a) => (
        <CircleMarker key={"alert-" + a.node} center={[a.lat!, a.lon!]} radius={12}
          pathOptions={{ color: "red", fillColor: "red", fillOpacity: 0.6 }}>
          <Popup>Alarm {a.node}: {Math.round(a.fireProbability * 100)} %</Popup>
        </CircleMarker>
      ))}
      {firefighters.map((f) => (
        <CircleMarker key={f.id} center={[f.lat, f.lon]} radius={7} pathOptions={{ color: "blue" }}>
          <Popup>Brandmand {f.id}</Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
