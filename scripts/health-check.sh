#!/usr/bin/env bash
# Health report for all Suchika services.
# Usage: bash scripts/health-check.sh
set -uo pipefail

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

UP=0; DOWN=0

# ── Helpers ──────────────────────────────────────────────────────────────────
check_http() {
  local name="$1" url="$2"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" 2>/dev/null || echo "000")
  if [[ "$code" =~ ^[2345][0-9][0-9]$ ]]; then
    printf "  ${GREEN}✓${NC}  %-22s  %s  ${GREEN}UP${NC} (HTTP %s)\n" "$name" "$url" "$code"
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
check_tcp  "PostgreSQL"        localhost 5432

echo -e "\n${BOLD}Backend Services${NC}"
check_http "Profile"           "http://localhost:8081/q/openapi"
check_http "Wealth"            "http://localhost:8082/q/openapi"
check_http "Health"            "http://localhost:8083/q/openapi"
check_http "Household"         "http://localhost:8084/q/openapi"
check_http "Web Gateway (BFF)" "http://localhost:8080/q/openapi"

echo -e "\n${BOLD}Frontend${NC}"
check_http "React Dev Server"  "http://localhost:3000"

# ── Summary ──────────────────────────────────────────────────────────────────
TOTAL=$(( UP + DOWN ))
echo ""
if [[ $DOWN -eq 0 ]]; then
  echo -e "${GREEN}${BOLD}All $TOTAL services UP.${NC}"
else
  echo -e "${YELLOW}${BOLD}$UP/$TOTAL services UP — $DOWN DOWN.${NC}"
fi
echo ""
