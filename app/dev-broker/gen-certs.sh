#!/usr/bin/env bash
# Generates a development CA and a broker certificate signed by it.
# Usage: dev-broker/gen-certs.sh [LAN_IP]
#   LAN_IP: your computer's Wi-Fi address, so a real phone can connect (optional).
# Re-running keeps the existing CA (so the app's bundled ca.crt stays valid) and re-issues the broker cert.
set -euo pipefail
cd "$(dirname "$0")"
LAN_IP="${1:-}"
mkdir -p certs

SAN="DNS:localhost,IP:127.0.0.1,IP:10.0.2.2"
if [ -n "$LAN_IP" ]; then SAN="$SAN,IP:$LAN_IP"; fi

if [ ! -f certs/ca.key ]; then
  openssl req -x509 -new -nodes -newkey rsa:2048 -sha256 -days 825 \
    -keyout certs/ca.key -out certs/ca.crt -subj "/CN=ForestFire Dev CA" \
    -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign"
fi

openssl req -new -nodes -newkey rsa:2048 -keyout certs/server.key -out certs/server.csr \
  -subj "/CN=forestfire-broker"
cat > certs/server.ext <<EOT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=$SAN
EOT
openssl x509 -req -in certs/server.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial \
  -out certs/server.crt -days 825 -sha256 -extfile certs/server.ext
chmod 644 certs/server.key   # mosquitto may run as a different user

echo "Broker cert SAN: $SAN"
echo "Next: cp dev-broker/certs/ca.crt app/src/main/res/raw/ca.crt"
