#!/usr/bin/env bash
# Checks that all tools required to build and run Suchika are present and at the right version.
# Run before the first build or after a fresh machine setup.
# Java/Node version floors come from scripts/services.json (single source of
# truth) via config.sh -- do not hardcode them here.
# Usage: bash scripts/check-prerequisites.sh
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/config.sh"
JAVA_FLOOR=$(suchika_version_floor javaHardFailBelow)
JAVA_TARGET=$(suchika_version_floor javaTarget)
NODE_FLOOR=$(suchika_version_floor nodeHardFailBelow)
NODE_TARGET=$(suchika_version_floor nodeTarget)

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

PASS=0; FAIL=0; WARN=0

ok()   { echo -e "  ${GREEN}✓${NC}  $*"; (( PASS++ )) || true; }
fail() { echo -e "  ${RED}✗${NC}  $*"; (( FAIL++ )) || true; }
warn() { echo -e "  ${YELLOW}~${NC}  $*"; (( WARN++ )) || true; }

# Compare two version strings (major.minor only)
version_ge() {
  local have="$1" need="$2"
  local have_maj have_min need_maj need_min
  have_maj=$(echo "$have" | cut -d. -f1)
  have_min=$(echo "$have" | cut -d. -f2 | sed 's/[^0-9].*//')
  need_maj=$(echo "$need" | cut -d. -f1)
  need_min=$(echo "$need" | cut -d. -f2 | sed 's/[^0-9].*//')
  (( have_maj > need_maj )) || (( have_maj == need_maj && have_min >= need_min ))
}

echo -e "\n${BOLD}${CYAN}=== Suchika Prerequisite Check ===${NC}\n"

# ── Git ───────────────────────────────────────────────────────────────────────
echo -e "${BOLD}Version Control${NC}"
if command -v git &>/dev/null; then
  GIT_VER=$(git --version | grep -oE '[0-9]+\.[0-9]+' | head -1)
  ok "git $GIT_VER"
else
  fail "git not found — install Git from https://git-scm.com"
fi

# ── Java ──────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Java (required: ${JAVA_FLOOR}+, target: ${JAVA_TARGET})${NC}"
if command -v java &>/dev/null; then
  JAVA_VER=$(java -version 2>&1 | grep -oE '"[0-9]+' | tr -d '"' | head -1)
  if (( JAVA_VER >= JAVA_TARGET )); then
    ok "Java $JAVA_VER"
  elif (( JAVA_VER >= JAVA_FLOOR )); then
    warn "Java $JAVA_VER found — CI/build.gradle.kts target Java $JAVA_TARGET; local tests may pass but CI could differ"
  else
    fail "Java $JAVA_VER found — Java below $JAVA_FLOOR is no longer supported. Install Temurin $JAVA_TARGET: https://adoptium.net"
  fi
else
  fail "java not found — install Temurin $JAVA_TARGET: https://adoptium.net"
fi

if command -v javac &>/dev/null; then
  JAVAC_VER=$(javac -version 2>&1 | grep -oE '[0-9]+' | head -1)
  ok "javac $JAVAC_VER"
else
  warn "javac not found — make sure JDK (not just JRE) is installed"
fi

# ── Gradle wrapper ────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Gradle${NC}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

if [[ -f "$ROOT/gradlew" ]]; then
  ok "gradlew present"
else
  fail "gradlew not found in repo root — repository may be incomplete"
fi

# ── Node.js ───────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}Node.js (required: ${NODE_FLOOR}+, CI uses ${NODE_TARGET})${NC}"
if command -v node &>/dev/null; then
  NODE_VER=$(node --version | tr -d 'v')
  NODE_MAJ=$(echo "$NODE_VER" | cut -d. -f1)
  if (( NODE_MAJ >= NODE_TARGET )); then
    ok "Node.js $NODE_VER (matches CI)"
  elif (( NODE_MAJ >= NODE_FLOOR )); then
    warn "Node.js $NODE_VER — works locally, but CI uses Node $NODE_TARGET; upgrade if you see CI failures"
  else
    fail "Node.js $NODE_VER — need ${NODE_FLOOR}+. Install via https://nodejs.org or nvm"
  fi
else
  fail "node not found — install Node.js $NODE_TARGET LTS: https://nodejs.org"
fi

# ── npm ───────────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}npm${NC}"
if command -v npm &>/dev/null; then
  NPM_VER=$(npm --version)
  ok "npm $NPM_VER"
else
  fail "npm not found — comes bundled with Node.js"
fi

# ── PostgreSQL ────────────────────────────────────────────────────────────────
echo -e "\n${BOLD}PostgreSQL${NC}"
if command -v psql &>/dev/null; then
  PG_VER=$(psql --version | grep -oE '[0-9]+\.[0-9]+' | head -1)
  PG_MAJ=$(echo "$PG_VER" | cut -d. -f1)
  if (( PG_MAJ >= 13 )); then
    ok "psql $PG_VER"
  else
    warn "psql $PG_VER — PostgreSQL 13+ recommended"
  fi
else
  warn "psql not found in PATH — install PostgreSQL 15+: https://www.postgresql.org/download"
fi

# Check if PostgreSQL is reachable
PG_PORT=$(suchika_db_field port)
if bash -c "echo > /dev/tcp/localhost/$PG_PORT" 2>/dev/null; then
  ok "PostgreSQL listening on localhost:$PG_PORT"
else
  warn "Nothing listening on localhost:$PG_PORT — start PostgreSQL before running any domain service"
fi

# ── curl (used by health-check script) ───────────────────────────────────────
echo -e "\n${BOLD}Optional tools${NC}"
if command -v curl &>/dev/null; then
  CURL_VER=$(curl --version | head -1 | grep -oE '[0-9]+\.[0-9]+' | head -1)
  ok "curl $CURL_VER (used by health-check.sh)"
else
  warn "curl not found — health-check.sh requires it"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
TOTAL=$(( PASS + FAIL + WARN ))
if [[ $FAIL -eq 0 ]]; then
  if [[ $WARN -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}All $TOTAL checks passed — environment is ready.${NC}"
  else
    echo -e "${YELLOW}${BOLD}$PASS passed, $WARN warnings, 0 failures — review warnings above.${NC}"
  fi
else
  echo -e "${RED}${BOLD}$FAIL checks failed, $WARN warnings — fix failures before building.${NC}"
  exit 1
fi
echo ""
