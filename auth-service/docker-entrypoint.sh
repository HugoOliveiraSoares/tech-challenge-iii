#!/bin/sh
set -eu

KEY_DIR="${JWT_KEYS_DIR:-/app/keys}"
PRIVATE_KEY="$KEY_DIR/app.key"
PUBLIC_KEY="$KEY_DIR/app.pub"

mkdir -p "$KEY_DIR"

if [ ! -s "$PRIVATE_KEY" ] || [ ! -s "$PUBLIC_KEY" ]; then
  openssl genpkey -algorithm RSA -out "$PRIVATE_KEY" -pkeyopt rsa_keygen_bits:2048
  openssl rsa -pubout -in "$PRIVATE_KEY" -out "$PUBLIC_KEY"
fi

exec java \
  -Djwt.private-key-location="file:$PRIVATE_KEY" \
  -Djwt.public-key-location="file:$PUBLIC_KEY" \
  -jar app.jar
