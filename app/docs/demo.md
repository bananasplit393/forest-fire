# Demoing the Forest Fire app

The laptop plays the backend: Mosquitto is the broker and `dev-broker/smoke-test.sh` publishes the fire
alerts the C# backend would send. The phone runs the app and reaches the laptop's broker through adb.

```
smoke-test.sh ──publish forest/zone1/alerts──▶ Mosquitto :8883 (TLS) ◀──adb reverse── phone app
      ▲                                                                                   │
      └────────────── prints forest/zone1/firefighters/+ (location every 10 s) ◀──────────┘
```

## One-time setup

```bash
sudo apt install -y mosquitto mosquitto-clients
sudo bash dev-broker/install-system-broker.sh     # TLS listener on 8883, prints "OK: ... listening"
```

The install script copies the certs into `/etc/mosquitto`, because Ubuntu's AppArmor profile blocks
Mosquitto from reading files anywhere else. Run it in your own terminal (it needs your sudo password).

In the app, open **Settings** and set host `127.0.0.1`, port `8883`, zone `zone1`, and any firefighter ID.

## Before each demo

1. **Broker running?**
   ```bash
   ss -ltn | grep 8883        # should print a LISTEN line
   ```
   If nothing prints: `sudo systemctl restart mosquitto` (or re-run the install script).

2. **Phone connected to adb.** USB cable, or wireless debugging:
   - Phone and laptop on the same network. Eduroam blocks device-to-device traffic: use the phone's
     hotspot.
   - First time only: Developer options → Wireless debugging → *Pair device with pairing code*, then
     `adb pair <IP>:<pair-port>` and type the code (keep the dialog open while pairing).
   - `adb connect <IP>:<port>` with the port from the main Wireless debugging screen.
   - Check: `adb devices` lists the phone.

   (`adb` is `~/Android/Sdk/platform-tools/adb` if it isn't on your PATH.)

3. **Forward the broker port to the phone.** Needed again after every adb reconnect; if the app is stuck
   on *Reconnecting*, this is almost always why.
   ```bash
   adb reverse tcp:8883 tcp:8883
   adb reverse --list          # should show tcp:8883 tcp:8883
   ```
   Host `127.0.0.1` is used because the dev certificate is only valid for `127.0.0.1`, `localhost` and
   `10.0.2.2`; the laptop's Wi-Fi IP would fail the TLS check.

4. **Install the latest app** (or press Run in Android Studio):
   ```bash
   ./gradlew :app:installDebug
   ```

5. **Phone ready:** alarm volume up (the buzzer uses the alarm volume, not the ringer), app open, tap
   **Go on duty**, wait for **Connected**.

## Running the demo

```bash
bash dev-broker/smoke-test.sh                 # defaults: zone1, host 127.0.0.1
bash dev-broker/smoke-test.sh zone2 127.0.0.1 # other zone / host
```

The script waits for **Enter** before each step:

| Step | The script sends | What the audience sees |
|---|---|---|
| 1 | Clears any old alarm | "No active alarm in Zone 1" |
| 2 | New fire | Buzzer loops, red alarm card. The terminal starts printing `[location]` lines every 10 s |
| 3 | Same fire, moved | Distance and arrow change; buzzer does not restart |
| 4 | You tap **End response**, then Enter: the same alert again | Nothing happens: a dismissed alert does not alarm again. Location lines stop |
| 5 | A newer fire | Alarm again |
| 6 | Backend clear (empty retained message) | Card disappears, location lines stop |

Things worth showing during step 2 or 5:

- The buzzer plays even with the phone on silent, and stops when you open the compass, tap **Silence**
  on the notification, or tap **Silence buzzer** on the card.
- **Navigate to fire** opens the compass: arrow, distance, bearing.
- **Google Maps** (on the compass, or **Open in Google Maps** on the card) opens walking directions.
- Lock the phone: the alarm notification still arrives (the app runs as a foreground service).

The fire coordinates in the script are near Aarhus (56.17, 10.19). If you demo somewhere else, edit the
three `alert ...` lines in `dev-broker/smoke-test.sh` so the distance and directions make sense.

## Sending alerts by hand

```bash
CA="--cafile dev-broker/certs/ca.crt -h 127.0.0.1 -p 8883"
mosquitto_pub $CA -r -t forest/zone1/alerts \
  -m "{\"active\":true,\"risk\":0.91,\"lat\":56.1720,\"lon\":10.1900,\"ts\":$(date +%s)}"
mosquitto_pub $CA -r -n -t forest/zone1/alerts                        # clear
mosquitto_sub $CA -t 'forest/+/firefighters/+' -v                       # watch locations
```

Each new fire needs a newer `ts` than the last one (`date +%s` does that).

## Troubleshooting

| Symptom | Fix |
|---|---|
| App stuck on *Connecting* / *Reconnecting* | `adb reverse tcp:8883 tcp:8883`; check host is `127.0.0.1` |
| `ss` shows nothing on 8883 | `sudo bash dev-broker/install-system-broker.sh` |
| `adb pair` says *connection refused* | Pairing dialog closed or wrong port; or the network isolates devices (use the hotspot) |
| No sound | Alarm volume down, or Do Not Disturb set to block alarms |
| Two phones keep disconnecting | They have the same firefighter ID; give each its own |
