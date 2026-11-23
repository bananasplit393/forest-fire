import { CircleMarker, MapContainer, Popup, TileLayer } from "react-leaflet";
import type { Firefighter, Reading } from "./api";

interface Props {
  readings: Reading[];
  firefighters: Firefighter[];
}

// en brandmand vises kun, hvis appen har sendt position inden for de sidste 30 sekunder
const CONNECTED_MS = 30_000;

export default function FireMap({ readings, firefighters }: Props) {
  // kun den nyeste måling (readings er nyeste først)
  const last = readings.find((r) => r.lat != null && r.lon != null);
  const connected = firefighters.filter((f) => Date.now() - new Date(f.receivedAt).getTime() < CONNECTED_MS);

  return (
    <MapContainer center={[56.0, 10.5]} zoom={7} className="map">
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="&copy; OpenStreetMap"
      />
      {last && (
        <CircleMarker key={last.id} center={[last.lat!, last.lon!]} radius={12}
          pathOptions={{ color: last.isFire ? "red" : "green", fillOpacity: 0.6 }}>
          <Popup>{last.node}: {Math.round((last.fireProbability ?? 0) * 100)} %</Popup>
        </CircleMarker>
      )}
      {connected.map((f) => (
        <CircleMarker key={f.id} center={[f.lat, f.lon]} radius={7} pathOptions={{ color: "blue" }}>
          <Popup>Brandmand {f.id}</Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
