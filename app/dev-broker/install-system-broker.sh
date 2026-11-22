#!/usr/bin/env bash
# Installs the dev TLS listener (port 8883) into the system Mosquitto service.
# Ubuntu's AppArmor profile only lets mosquitto read /etc/mosquitto, so the
# certs and config must live there. Run from the project root:
#   sudo bash dev-broker/install-system-broker.sh
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p /etc/mosquitto/certs
cp certs/ca.crt certs/server.crt certs/server.key /etc/mosquitto/certs/
chown mosquitto: /etc/mosquitto/certs/server.key
chmod 600 /etc/mosquitto/certs/server.key

cat > /etc/mosquitto/conf.d/forestfire.conf <<EOF
listener 8883
cafile /etc/mosquitto/certs/ca.crt
certfile /etc/mosquitto/certs/server.crt
keyfile /etc/mosquitto/certs/server.key
allow_anonymous true
EOF

systemctl restart mosquitto
sleep 1
if ss -ltn | grep -q ':8883'; then
  echo "OK: Mosquitto is listening on 8883 (TLS)"
else
  echo "FAILED: nothing on 8883. Last log lines:"
  journalctl -u mosquitto -n 20 --no-pager
  exit 1
fi
