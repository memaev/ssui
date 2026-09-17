#!/usr/bin/env bash
# Puts the original spec example (backend seed) back as the "home" screen.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
SEED="$(dirname "$0")/../ssui-backend/src/main/resources/seed/home.json"
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X PUT -H 'Content-Type: application/json' \
  --data @"$SEED" "$BASE_URL/api/v1/screens/home")
[ "$STATUS" = "200" ] && echo "OK: 'home' restored to the seed example." || { echo "FAILED ($STATUS)"; exit 1; }
