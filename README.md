# Producer test

Test that the producer sends sensor readings to the CoAP server and that the
server forwards them to MQTT:

```text
Producer -> CoAP POST /readings (UDP 5683) -> MQTT broker (TCP 1883)
                                             |
                                             v
                                   forest-fire/readings
```

The commands below are the local testing sequence used successfully. Run each
step in a separate terminal so the services can run at the same time.

## Prerequisites

- Node.js and npm.
- Mosquitto broker and command-line clients (`mosquitto` and `mosquitto_sub`).
- Project dependencies installed. If this is a fresh checkout, run `npm install`
  once in each of the `coap-server` and `producers` directories.

The directory-changing commands below assume the repository is located at
`/.../forest-fire`. Adjust that path if needed.

## 1. Start the MQTT broker

In terminal 1:

```bash
mosquitto -v
```

This starts the MQTT broker on its default port, `1883`. The `-v` flag enables
verbose logging so you can see connections and MQTT activity.

Skip this command if a broker is already running on `localhost:1883`. The CoAP
server needs the broker because it waits for MQTT to acknowledge publication
before sending a successful CoAP response.

## 2. Start the CoAP server

In terminal 2:

```bash
cd /.../forest-fire/coap-server
npm start
```

`npm start` runs `tsx server.ts`, which starts
the CoAP listener and connects to the MQTT broker.

Expected startup messages (their order may vary):

```text
CoAP server listening on udp/5683
MQTT connected to mqtt://localhost:1883
```

The server accepts JSON sensor readings sent with `POST` to
`coap://localhost:5683/readings`, validates them, and publishes accepted readings
to the MQTT topic `forest-fire/readings` with QoS 1.

## 3. Start sending producer readings

In terminal 3:

```bash
cd /.../forest-fire/producers
npm run send
```

`npm run send` runs `tsx send.ts`. It generates a sensor reading and sends it
immediately, then repeats every five seconds. Each reading is sent as JSON in a
confirmable CoAP `POST` request to `coap://localhost:5683/readings`.

The producer should print `2.01` followed by the reading. The CoAP server should
print:

```text
Published to forest-fire/readings:
```

followed by the same reading. Compare the timestamp (`ts`) and measurement values
in both terminals. A `2.01` response means the server accepted the reading and
the MQTT broker acknowledged publication.

**Use `npm run send`, not `npm start`, in the producer directory.** The producer's
`npm start` command only generates and prints a reading locally; it does not send
anything to the CoAP server.

## 4. Independently check MQTT delivery

In terminal 4:

```bash
mosquitto_sub -h localhost -t forest-fire/readings -v
```

This subscribes to the broker and prints incoming readings:

- `-h localhost` selects the local MQTT broker.
- `-t forest-fire/readings` selects the topic used by the CoAP server.
- `-v` prints the topic name alongside each message payload.

Leave it running until the next producer reading arrives. Messages are not
retained, so this command will not show readings published before it subscribed.

Expected output has this shape (values will vary):

```text
forest-fire/readings {"ts":1794340800,"temp":20.1,"humidity":65,"pm10":12.3,"pm25":7.4,"lat":55.67,"lon":12.56}
```

Matching readings in the producer, CoAP server, and subscriber confirm the full
producer-to-CoAP-to-MQTT path.

## Troubleshooting and stopping

- **`MQTT error: connect ECONNREFUSED`:** check that the MQTT broker is running on
  `localhost:1883`. Without it, the server may leave CoAP requests waiting instead
  of returning success.
- **Broker reports that the address is already in use:** a broker may already be
  running. Use that instance instead of starting another on the same port.
- **No producer responses:** check that the CoAP server is running and listening
  on UDP port `5683`, and that the producer uses `/readings`.
- **CoAP `4.00`:** the server rejected malformed JSON or an invalid reading.
- **CoAP `4.04` / `4.05`:** the request used the wrong path / method.
- **CoAP `5.03`:** MQTT publication reported an error; check the server logs.

Press **Ctrl+C** in each test terminal to stop its process. Do not stop a shared
or pre-existing broker that other applications are using.
