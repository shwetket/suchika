#!/usr/bin/env bash
# Health report for all Suchika services.
# Hits the real Quarkus /q/health endpoint (quarkus-smallrye-health) on each
# backend service and only counts a genuine HTTP 200 with body status "UP" as
# UP -- previously this hit /q/openapi and treated ANY response, including a
# 500, as "UP", which meant a broken service could still report healthy.
# Ports/URLs come from scripts/services.json (single source of truth) via
# config.sh. Also shows the PID-registry entry for each service, if one exists
# (see service-registry.sh) -- informational only, does not affect UP/DOWN.
# Usage: bash scripts/health-check.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/config.sh"
. "$SCRIPT_DIR/service-registry.sh"

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

UP=0; DOWN=0

pid_suffix() {
  local svc="$1" pid
  pid=$(suchika_get_running_pid "$svc" 2>/dev/null) || { echo ""; return; }
  echo "  [PID $pid]"
}

# ── Helpers ──────────────────────────────────────────────────────────────────
check_health() {
  local name="$1" url="$2" svc="$3"
  local body status code suffix
  suffix=$(pid_suffix "$svc")
  body=$(curl -s --max-time 3 "$url" 2>/dev/null || echo "")
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null || echo "000")
  status=$(echo "$body" | grep -oP '"status"\s*:\s*"\K[^"]*' || echo "")
  if [[ "$code" == "200" && "$status" == "UP" ]]; then
    printf "  ${GREEN}✓${NC}  %-22s  %s  ${GREEN}UP${NC} (HTTP 200, status=UP)%s\n" "$name" "$url" "$suffix"
    (( UP++ )) || true
  else
    printf "  ${RED}✗${NC}  %-22s  %s  ${RED}DOWN${NC} (HTTP %s, status=%s)%s\n" "$name" "$url" "$code" "${status:-none}" "$suffix"
    (( DOWN++ )) || true
  fi
}

check_http() {
  # Plain reachability check (used for the frontend, which has no /q/health).
  local name="$1" url="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null || echo "000")
  if [[ "$code" == "200" ]]; then
    printf "  ${GREEN}✓${NC}  %-22s  %s  ${GREEN}UP${NC} (HTTP 200)\n" "$name" "$url"
    (( UP++ )) || true
  else
    printf "  ${RED}✗${NC}  %-22s  %s  ${RED}DOWN${NC}\n" "$name" "$url"
    (( DOWN++ )) || true
  fi
}

check_tcp() {
  local name="$1" host="$2" port="$3"
  if bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null; then
    printf "  ${GREEN}✓${NC}  %-22s  %s:%s  ${GREEN}UP${NC}\n" "$name" "$host" "$port"
    (( UP++ )) || true
  else
    printf "  ${RED}✗${NC}  %-22s  %s:%s  ${RED}DOWN${NC}\n" "$name" "$host" "$port"
    (( DOWN++ )) || true
  fi
}

# ── Report ───────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}${CYAN}=== Suchika Health Report === $(date '+%Y-%m-%d %H:%M:%S')${NC}\n"

echo -e "${BOLD}Database${NC}"
check_tcp "PostgreSQL" localhost "$(suchika_db_field port)"

echo -e "\n${BOLD}Backend Services${NC}"
for svc in profile wealth health household gateway; do
  port=$(suchika_svc_field "$svc" port)
  label="$svc"
  [[ "$svc" == "gateway" ]] && label="Web Gateway (BFF)"
  check_health "$label" "http://localhost:$port/q/health" "$svc"
done

echo -e "\n${BOLD}Frontend${NC}"
web_port=$(suchika_svc_field web port)
check_http "React Dev Server" "http://localhost:$web_port"

# ── Summary ──────────────────────────────────────────────────────────────────
TOTAL=$(( UP + DOWN ))
echo ""
if [[ $DOWN -eq 0 ]]; then
  echo -e "${GREEN}${BOLD}All $TOTAL services UP.${NC}"
else
  echo -e "${YELLOW}${BOLD}$UP/$TOTAL services UP — $DOWN DOWN.${NC}"
fi
echo ""
