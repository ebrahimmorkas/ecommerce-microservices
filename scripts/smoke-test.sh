#!/usr/bin/env bash
# End-to-end smoke test of the order saga through the API gateway.
# Usage: docker compose up -d --build && ./scripts/smoke-test.sh
set -uo pipefail

GATEWAY=${GATEWAY:-http://localhost:8080}
MAILPIT=${MAILPIT:-http://localhost:8025}
failures=0

pass() { echo "  PASS  $1"; }
fail() { echo "  FAIL  $1"; failures=$((failures + 1)); }

json_field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
stock_of() { curl -s "$GATEWAY/api/products/$1" | grep -o '"quantityAvailable":[0-9]*' | cut -d: -f2; }

place_order() {
  # Retries while the platform is still starting up (gateway 503 / circuit breaker open)
  local body=$1 response
  for _ in $(seq 1 30); do
    response=$(curl -s -X POST "$GATEWAY/api/orders" -H "Content-Type: application/json" -d "$body")
    if echo "$response" | grep -q '"orderNumber"'; then
      echo "$response" | json_field orderNumber
      return
    fi
    sleep 3
  done
}

await_final_status() {
  local status
  for _ in $(seq 1 30); do
    status=$(curl -s "$GATEWAY/api/orders/$1" | json_field status)
    [ "$status" != "PENDING" ] && { echo "$status"; return; }
    sleep 1
  done
  echo "PENDING"
}

echo "Waiting for the gateway..."
until [ "$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/api/products?size=1")" = "200" ]; do sleep 3; done

echo "1) Happy path: order is confirmed and stock is deducted"
before=$(stock_of IPHONE-15)
order=$(place_order '{"customerEmail":"smoke@example.com","items":[{"skuCode":"IPHONE-15","quantity":1}]}')
[ "$(await_final_status "$order")" = "CONFIRMED" ] && pass "order $order CONFIRMED" || fail "order $order not confirmed"
[ "$(curl -s "$GATEWAY/api/payments/$order" | json_field status)" = "COMPLETED" ] && pass "payment COMPLETED" || fail "payment not completed"
[ "$(stock_of IPHONE-15)" -eq $((before - 1)) ] && pass "stock deducted" || fail "stock not deducted"

echo "2) Payment declined: order is cancelled and reserved stock is released (compensation)"
before=$(stock_of MACBOOK-AIR)
order=$(place_order '{"customerEmail":"smoke@example.com","items":[{"skuCode":"MACBOOK-AIR","quantity":5}]}')
[ "$(await_final_status "$order")" = "CANCELLED" ] && pass "order $order CANCELLED" || fail "order $order not cancelled"
sleep 2
[ "$(stock_of MACBOOK-AIR)" -eq "$before" ] && pass "stock released back to $before" || fail "stock not released"

echo "3) Insufficient stock: order is cancelled without touching stock"
before=$(stock_of PIXEL-9)
order=$(place_order '{"customerEmail":"smoke@example.com","items":[{"skuCode":"PIXEL-9","quantity":999}]}')
[ "$(await_final_status "$order")" = "CANCELLED" ] && pass "order $order CANCELLED" || fail "order $order not cancelled"
[ "$(stock_of PIXEL-9)" -eq "$before" ] && pass "stock unchanged" || fail "stock changed"

echo "4) Cross-cutting concerns"
curl -s -D - -o /dev/null "$GATEWAY/api/products/IPHONE-15" | grep -qi "X-Correlation-Id" \
  && pass "gateway returns X-Correlation-Id" || fail "missing X-Correlation-Id"
curl -s "$MAILPIT/api/v1/messages" | grep -q "smoke@example.com" \
  && pass "customer emails delivered to Mailpit" || fail "no customer emails"

echo
[ "$failures" -eq 0 ] && echo "All checks passed." || echo "$failures check(s) failed."
exit "$failures"
