#!/bin/sh
# Lab 04 - Security (Secure Channel): LOCAL TEST CERTIFICATE ONLY.
#
# Generates a short-lived self-signed certificate the first time the container
# starts, so no private key is ever baked into the image or committed to git.
# The certificate is stored in /etc/nginx/certs, which is a named volume
# (frontend_certs) so it survives container restarts and `curl --cacert` stays
# reproducible.
#
# Never use this certificate outside a local/dev environment.
set -eu

CERT_DIR=/etc/nginx/certs
CERT="$CERT_DIR/dev.crt"
KEY="$CERT_DIR/dev.key"

if [ -f "$CERT" ] && [ -f "$KEY" ]; then
    echo "[dev-tls] reusing existing dev certificate in $CERT_DIR"
    exit 0
fi

echo "[dev-tls] generating self-signed dev certificate (30 days) in $CERT_DIR"
mkdir -p "$CERT_DIR"

# SANs cover localhost and the LAN IP used by the team demo. Add more IPs/DNS
# names here if the demo host changes.
openssl req -x509 -nodes -newkey rsa:2048 -days 30 \
    -keyout "$KEY" \
    -out "$CERT" \
    -subj "/O=ServiYa Lab 04/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:serviya.local,IP:127.0.0.1,IP:192.168.1.2"

chmod 600 "$KEY" "$CERT"
echo "[dev-tls] certificate ready:"
openssl x509 -in "$CERT" -noout -subject -dates
