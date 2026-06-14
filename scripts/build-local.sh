#!/usr/bin/env bash
# Mirrors the CI pipeline exactly — run before committing to catch failures locally.
# Usage: bash scripts/build-local.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

step() { echo -e "\n${CYAN}==> $*${NC}"; }
ok()   { echo -e "${GREEN}✓ $*${NC}"; }

START=$(date +%s)

# ── 1. Migration location check ──────────────────────────────────────────────
step "Checking migration file locations..."
bash scripts/check-migrations-location.sh
ok "Migration location check passed"

# ── 2. Backend tests (includes ArchUnit) ─────────────────────────────────────
step "Running backend tests (includes ArchUnit)..."
./gradlew test --continuous=false
ok "Backend tests passed"

# ── 3. Frontend ───────────────────────────────────────────────────────────────
cd web

step "Installing frontend dependencies..."
npm install
ok "Dependencies installed"

step "Generating API types from OpenAPI contract..."
npm run generate:api
ok "API types generated"

step "Linting frontend..."
npm run lint
ok "Lint passed"

step "Fixing frontend formatting (auto-fix before commit)..."
npm run format
ok "Formatting applied"

step "Running frontend unit tests..."
npm run test:ci
ok "Frontend tests passed"

step "Building React frontend..."
npm run build
ok "Frontend build passed"

cd "$ROOT"

ELAPSED=$(( $(date +%s) - START ))
echo -e "\n${GREEN}All checks passed in ${ELAPSED}s — safe to commit.${NC}"
