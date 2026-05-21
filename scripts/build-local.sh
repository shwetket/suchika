#!/usr/bin/env bash
# Ultimate pre-commit build verification for Suchika.
# Builds each domain one by one in dependency order, checks frontend lint/format/tests/build,
# runs ArchUnit, and optionally runs SonarQube analysis.
#
# Usage:
#   bash scripts/build-local.sh
#   bash scripts/build-local.sh --skip-sonar
#   bash scripts/build-local.sh --skip-frontend
set -euo pipefail

SKIP_SONAR=false
SKIP_FRONTEND=false
for arg in "$@"; do
  case $arg in
    --skip-sonar)    SKIP_SONAR=true ;;
    --skip-frontend) SKIP_FRONTEND=true ;;
    *) echo "Unknown flag: $arg" >&2; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

step() { printf "\n${CYAN}==> %s${NC}\n" "$*"; }
ok()   { printf "  ${GREEN}[OK] %s${NC}\n" "$*"; }
warn() { printf "  ${YELLOW}[!]  %s${NC}\n" "$*"; }
fail() { printf "  ${RED}[X]  %s${NC}\n" "$*"; exit 1; }

elapsed() {
  local s=$(( $(date +%s) - $1 ))
  if (( s >= 60 )); then printf "%dm%02ds" $(( s/60 )) $(( s%60 ))
  else printf "%ds" "$s"; fi
}

TOTAL_START=$(date +%s)

run_gradle() {
  local label="$1"; shift
  local t=$(date +%s)
  step "$label"
  ./gradlew "$@"
  ok "$label  ($(elapsed $t))"
}

# ── 0. Pre-flight: check migration files are not in adapters ──────────────────
step "Pre-flight: migration file location check"
bash scripts/check-migrations-location.sh
ok "Migration locations clean"

# ── 1. Shared — AppLogger, exception hierarchy, ArchUnit fitness functions ─────
run_gradle "shared (ArchUnit + exception hierarchy)" \
  :shared:build :shared:test

# ── 2. Profile — MUST run first; every other domain FKs into profile.profile ───
run_gradle "profile / domain layer" \
  :application:domain:profile:domain:build \
  :application:domain:profile:domain:test

run_gradle "profile / ports layer" \
  :application:domain:profile:ports:build \
  :application:domain:profile:ports:test

run_gradle "profile / adapters layer" \
  :application:domain:profile:adapters:build \
  :application:domain:profile:adapters:test

# ── 3. Health domain ───────────────────────────────────────────────────────────
run_gradle "health / domain layer" \
  :application:domain:health:domain:build \
  :application:domain:health:domain:test

run_gradle "health / ports layer" \
  :application:domain:health:ports:build \
  :application:domain:health:ports:test

run_gradle "health / adapters layer" \
  :application:domain:health:adapters:build \
  :application:domain:health:adapters:test

# ── 4. Household domain ────────────────────────────────────────────────────────
run_gradle "household / domain layer" \
  :application:domain:household:domain:build \
  :application:domain:household:domain:test

run_gradle "household / ports layer" \
  :application:domain:household:ports:build \
  :application:domain:household:ports:test

run_gradle "household / adapters layer" \
  :application:domain:household:adapters:build \
  :application:domain:household:adapters:test

# ── 5. Wealth domain ───────────────────────────────────────────────────────────
run_gradle "wealth / domain layer" \
  :application:domain:wealth:domain:build \
  :application:domain:wealth:domain:test

run_gradle "wealth / ports layer" \
  :application:domain:wealth:ports:build \
  :application:domain:wealth:ports:test

run_gradle "wealth / adapters layer" \
  :application:domain:wealth:adapters:build \
  :application:domain:wealth:adapters:test

# ── 6. Web-gateway (BFF — aggregates domain REST APIs, no direct DB) ───────────
run_gradle "web-gateway (BFF aggregator)" \
  :application:web-gateway:build \
  :application:web-gateway:test

# ── 7. Frontend ────────────────────────────────────────────────────────────────
if [ "$SKIP_FRONTEND" = false ]; then
  cd web

  step "Frontend: install dependencies"
  t=$(date +%s)
  if ! npm ci --prefer-offline 2>&1 | tail -3; then
    warn "npm ci failed (package-lock.json may be stale) — falling back to npm install"
    npm install
  fi
  ok "Dependencies ready  ($(elapsed $t))"

  step "Frontend: generate API types from OpenAPI contract"
  t=$(date +%s)
  if npm run generate:api 2>&1; then
    ok "API types generated  ($(elapsed $t))"
  else
    warn "API generation skipped — contract YAML not yet changed or gateway not built"
  fi

  step "Frontend: ESLint"
  t=$(date +%s)
  npm run lint
  ok "Lint clean  ($(elapsed $t))"

  step "Frontend: Prettier format check"
  t=$(date +%s)
  if ! npm run format:check; then
    fail "Formatting violations — run 'npm run format' in web/, re-stage changed files, then re-run this script"
  fi
  ok "Formatting clean  ($(elapsed $t))"

  step "Frontend: unit tests"
  t=$(date +%s)
  npm run test:ci
  ok "Tests passed  ($(elapsed $t))"

  step "Frontend: production build"
  t=$(date +%s)
  npm run build
  ok "Build OK  ($(elapsed $t))"

  cd "$ROOT"
fi

# ── 8. SonarQube analysis ──────────────────────────────────────────────────────
if [ "$SKIP_SONAR" = false ]; then
  step "SonarQube analysis"
  export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

  if ! curl -sf --max-time 3 http://localhost:9000/api/system/status > /dev/null 2>&1; then
    warn "SonarQube not running at http://localhost:9000 — skipping"
    warn "Start it:  bin/linux-x86-64/sonar.sh start"
    warn "Then run:  sonar-scanner  (from the repo root)"
  elif ! command -v sonar-scanner &> /dev/null; then
    warn "sonar-scanner not in PATH — skipping"
    warn "Install:  npm install -g sonar-scanner"
  else
    t=$(date +%s)
    sonar-scanner
    ok "SonarQube analysis complete  ($(elapsed $t))"
    printf "     ${GRAY}Dashboard: http://localhost:9000/dashboard?id=suchika${NC}\n"
  fi
fi

# ── Summary ────────────────────────────────────────────────────────────────────
TOTAL=$(( $(date +%s) - TOTAL_START ))
printf "\n${GREEN}  All checks passed in %dm%02ds -- safe to commit.${NC}\n" \
  $(( TOTAL/60 )) $(( TOTAL%60 ))
