import { createServer } from "coap";
import { connect } from "mqtt";

const COAP_PORT = Number(process.env.COAP_PORT ?? 5683);
const MQTT_URL = process.env.MQTT_URL ?? "mqtt://localhost:1883";
const MQTT_TOPIC = process.env.MQTT_TOPIC ?? "forest-fire/readings";

const NUMERIC_FIELDS = ["temp", "humidity", "pm10", "pm25", "lat", "lon"];

function isValidReading(value: unknown): value is Record<string, number> {
  if (typeof value !== "object" || value === null) return false;
  const reading = value as Record<string, unknown>;
  if (!Number.isInteger(reading.ts)) return false;
  return NUMERIC_FIELDS.every(
    (key) => reading[key] === undefined || Number.isFinite(reading[key]),
  );
}

const client = connect(MQTT_URL);
client.on("connect", () => console.log(`MQTT connected to ${MQTT_URL}`));
client.on("error", (err) => console.error("MQTT error:", err.message));

const server = createServer((req, res) => {
  if (req.url !== "/readings") {
    res.code = "4.04";
    return res.end();
  }
  if (req.method !== "POST") {
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

  client.publish(MQTT_TOPIC, JSON.stringify(reading), { qos: 1 }, (err) => {
    if (err) {
      console.error("MQTT publish failed:", err.message);
      res.code = "5.03";
    } else {
      console.log(`Published to ${MQTT_TOPIC}:`, reading);
      res.code = "2.01";
    }
    res.end();
  });
});

server.listen(COAP_PORT, () =>
  console.log(`CoAP server listening on udp/${COAP_PORT}`),
);
