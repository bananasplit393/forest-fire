#!/usr/bin/env bash
# Interactive smoke test of the firefighter app against the local TLS broker.
# Plays the backend: publishes alarms on forest/<zone>/alerts and shows the
# firefighter locations the app publishes. Press Enter to advance each step.
# Usage: dev-broker/smoke-test.sh [zone] [host]
set -euo pipefail
cd "$(dirname "$0")"
ZONE="${1:-zone1}"
HOST="${2:-127.0.0.1}"
MQ=(--cafile certs/ca.crt -h "$HOST" -p 8883)

alert() { # risk lat lon
  mosquitto_pub "${MQ[@]}" -r -q 1 -t "forest/$ZONE/alerts" \
    -m "{\"active\":true,\"risk\":$1,\"lat\":$2,\"lon\":$3,\"ts\":$(date +%s)}"
}
step() { echo; echo "== $1"; read -r -p "   (Enter to run) "; }

echo "Watching firefighter locations on forest/$ZONE/firefighters/+ ..."
mosquitto_sub "${MQ[@]}" -t "forest/$ZONE/firefighters/+" -v | sed 's/^/   [location] /' &
SUB=$!
trap 'kill $SUB 2>/dev/null; true' EXIT

step "1. Clear any old alarm (app should show 'No active alarm')"
mosquitto_pub "${MQ[@]}" -r -n -t "forest/$ZONE/alerts"

step "2. NEW FIRE ~1.3 km away: alarm sound + red card; locations should print here every 10 s"
alert 0.91 56.1720 10.1900

step "3. POSITION UPDATE (fire moved): no new sound, compass distance/arrow changes"
alert 0.95 56.1750 10.1850

step "4. Now tap 'End response' in the app (locations stop). Then press Enter to re-send the SAME retained alert"
echo "   Nothing should alarm: the dismissed alert is ignored (toggle airplane mode to force a reconnect if you like)."

step "5. A NEWER alarm after dismissal: should alarm again"
alert 0.88 56.1700 10.2050

step "6. Backend CLEAR (empty retained): card disappears, locations stop"
mosquitto_pub "${MQ[@]}" -r -n -t "forest/$ZONE/alerts"

echo; echo "Done. Ctrl+C or wait to exit."; sleep 3
