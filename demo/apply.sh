#!/usr/bin/env bash
# Uploads a screen JSON to the backend under a given screen name.
#
#   ./demo/apply.sh                       # demo-screen.json -> "home" (what the app shows)
#   ./demo/apply.sh demo                  # demo-screen.json -> "demo"
#   ./demo/apply.sh home my-screen.json   # any file -> "home"
#
# Env: BASE_URL (default http://localhost:8080)
set -euo pipefail
NAME="${1:-home}"
FILE="${2:-$(dirname "$0")/demo-screen.json}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

STATUS=$(curl -s -o /tmp/ssui_apply_response.json -w '%{http_code}' \
  -X PUT -H 'Content-Type: application/json' --data @"$FILE" \
  "$BASE_URL/api/v1/screens/$NAME")

if [ "$STATUS" = "200" ]; then
  echo "OK: '$FILE' is now served as screen '$NAME'. Tap refresh in the app."
else
  echo "FAILED ($STATUS):"; cat /tmp/ssui_apply_response.json; echo; exit 1
fi
