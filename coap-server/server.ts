import { createServer } from "coap";
import { connect } from "mqtt";

const COAP_PORT = Number(process.env.COAP_PORT ?? 5683);
const MQTT_URL = process.env.MQTT_URL ?? "mqtt://localhost:1883";
// SensorData-topic'et. {nodeId} erstattes med sensor-nodens id, fx sensors/node-1/data
const MQTT_TOPIC = process.env.MQTT_TOPIC ?? "sensors/{nodeId}/data";
// node-id til POST /readings, når målingen ikke selv har et "node"-felt
const DEFAULT_NODE_ID = process.env.DEFAULT_NODE_ID ?? "unknown";
// login til Mosquitto (samme som MQTT_USERNAME/MQTT_PASSWORD i api/.env). Tomt = anonym, fx `mosquitto -v`
const MQTT_USERNAME = process.env.MQTT_USERNAME || undefined;
const MQTT_PASSWORD = process.env.MQTT_PASSWORD || undefined;

// et node-id må ikke indeholde MQTT-tegnene / + #
const isValidNodeId = (id: string) => id.length > 0 && !/[/+#]/.test(id);

const NUMERIC_FIELDS = ["temp", "humidity", "pm10", "pm25", "lat", "lon"];

function isValidReading(value: unknown): value is Record<string, number> {
  if (typeof value !== "object" || value === null) return false;
  const reading = value as Record<string, unknown>;
  if (!Number.isInteger(reading.ts)) return false;
  return NUMERIC_FIELDS.every(
    (key) => reading[key] === undefined || Number.isFinite(reading[key]),
  );
}

const client = connect(MQTT_URL, {
  username: MQTT_USERNAME,
  password: MQTT_PASSWORD,
});
client.on("connect", () => console.log(`MQTT connected to ${MQTT_URL}`));
client.on("error", (err) => console.error("MQTT error:", err.message));

const server = createServer((req, res) => {
  // PUT /s/{id}: node-id i stien (som i sekvensdiagrammet)
  // POST /readings: node-id fra "node" i body'en, ellers DEFAULT_NODE_ID (så de nuværende producere virker)
  const nodeMatch = /^\/s\/([^/]+)$/.exec(req.url);
  if (!nodeMatch && req.url !== "/readings") {
    res.code = "4.04";
    return res.end();
  }
  if ((nodeMatch && req.method !== "PUT") || (!nodeMatch && req.method !== "POST")) {
    res.code = "4.05";
    return res.end();
  }

  let reading: unknown;
  try {
    reading = JSON.parse(req.payload.toString());
  } catch {
    res.code = "4.00";
    return res.end();
  }
  if (!isValidReading(reading)) {
    res.code = "4.00";
    return res.end();
  }

  const bodyNode = (reading as Record<string, unknown>).node;
  let nodeId: string;
  try {
    nodeId = nodeMatch
      ? decodeURIComponent(nodeMatch[1] ?? "")
      : typeof bodyNode === "string" ? bodyNode : DEFAULT_NODE_ID;
  } catch {
    nodeId = ""; // ugyldig %-kodning i stien
  }
  if (!isValidNodeId(nodeId)) {
    res.code = "4.00";
    return res.end();
  }

  // SensorData = {node, ts, temp, humidity, pm10, pm25, lat, lon}, QoS 1 så hver måling når backenden
  const topic = MQTT_TOPIC.replace("{nodeId}", nodeId);
  const sensorData = { ...reading, node: nodeId };
  client.publish(topic, JSON.stringify(sensorData), { qos: 1 }, (err) => {
    if (err) {
      console.error("MQTT publish failed:", err.message);
      res.code = "5.03";
    } else {
      console.log(`Published to ${topic}:`, sensorData);
      res.code = "2.01";
    }
    res.end();
  });
});

server.listen(COAP_PORT, () =>
  console.log(`CoAP server listening on udp/${COAP_PORT}`),
);
