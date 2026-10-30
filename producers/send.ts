import { request } from "coap";
import { produce } from "./main/produce";

const COAP_URL = process.env.COAP_URL ?? "coap://localhost:5683/readings";
const INTERVAL_MS = Number(process.env.INTERVAL_MS ?? 5000);

async function send() {
  const reading = await produce();
  const url = new URL(COAP_URL);

  const req = request({
    hostname: url.hostname,
    port: Number(url.port || 5683),
    pathname: url.pathname,
    method: "POST",
    confirmable: true,
  });
  req.setOption("Content-Format", "application/json");
  req.on("response", (res) => console.log(`${res.code}`, reading));
  req.on("error", (err) => console.error("CoAP error:", err.message));
  req.end(Buffer.from(JSON.stringify(reading)));
}

send();
setInterval(send, INTERVAL_MS);
