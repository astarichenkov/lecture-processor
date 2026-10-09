#!/usr/bin/env bash
# Verifies a deployed private-ai-chat service.
# Usage: ./verify.sh [base-url] [api-key]
set -uo pipefail

BASE="${1:-http://127.0.0.1:8081}"
KEY="${2:-${CHAT_API_KEY:-}}"
AUTH=()
[ -n "$KEY" ] && AUTH=(-H "X-API-Key: $KEY")

pass=0
fail=0
check() {
  local name="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "PASS  $name (HTTP $actual)"; pass=$((pass+1))
  else
    echo "FAIL  $name (expected $expected, got $actual)"; fail=$((fail+1))
  fi
}

code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

check "health" 200 "$(code "$BASE/api/health")"
check "chat authenticated" 200 "$(code "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d '{"message":"ping"}' "$BASE/api/chat")"
check "oversized rejected" 400 "$(code "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"message\":\"$(printf 'x%.0s' {1..3000})\"}" "$BASE/api/chat")"
if [ -n "$KEY" ]; then
  check "unauthenticated rejected" 401 "$(code -H 'Content-Type: application/json' \
    -d '{"message":"ping"}' "$BASE/api/chat")"
fi

echo "---"
echo "Passed: $pass, Failed: $fail"
[ "$fail" -eq 0 ]
