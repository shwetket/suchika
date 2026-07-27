#!/bin/bash
# setup.sh
# Runs on first codespace creation (postCreateCommand).
# Assumes download-deps.sh already ran during prebuild.
# Safe to re-run.

set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Setting up Suchika development environment..."

# ── PostgreSQL client (psql, pg_isready) ──────────────────────────────────────
# The base Java devcontainer image does not include psql. Install it so that
# database connectivity checks and the bootstrap step below work correctly.
if ! command -v psql >/dev/null 2>&1; then
  echo "==> Installing postgresql-client..."
  sudo apt-get update -qq && sudo apt-get install -y postgresql-client
fi

# ── .env ─────────────────────────────────────────────────────────────────────
if [ ! -f application/finance/.env ]; then
  cp infrastructure/local/.env.template application/finance/.env
fi
# Codespaces: DB lives in the 'db' container, not localhost
sed -i 's|DB_URL=.*|DB_URL=jdbc:postgresql://db:5432/app_db|' application/finance/.env
sed -i 's|DB_PASSWORD=.*|DB_PASSWORD=local_dev_only|' application/finance/.env
# Remove TZ from .env — it is set as a container-level env var (takes precedence anyway)
sed -i '/^TZ=/d' application/finance/.env

# ── Ensure gradlew is executable ──────────────────────────────────────────────
chmod +x gradlew

# ── Wait for PostgreSQL ──────────────────────────────────────────────────────
echo "==> Waiting for PostgreSQL at db:5432..."
timeout 60 bash -c 'until pg_isready -h db -U postgres >/dev/null 2>&1; do sleep 2; done'

# ── Bootstrap database ────────────────────────────────────────────────────────
echo "==> Bootstrapping database..."
PGPASSWORD="${DB_PASSWORD:-local_dev_only}" psql -h db -U postgres -c "CREATE DATABASE app_db;" 2>/dev/null \
  && echo "  Created app_db" \
  || echo "  app_db already exists — skipping"

PGPASSWORD="${DB_PASSWORD:-local_dev_only}" psql -h db -U postgres -d app_db \
  -f "$ROOT/application/flyway/00_bootstrap.sql" 2>/dev/null \
  && echo "  Bootstrap SQL applied" \
  || echo "  Bootstrap already applied — skipping"

# ── Profile schema migrations ─────────────────────────────────────────────────
# All other domains FK into profile.profile, so profile Flyway migrations must
# be applied before running tests for health, wealth, or household. The domain
# services normally run these on startup, but they may not be started yet.
echo "==> Applying profile schema migrations..."
PGPASSWORD="${DB_PASSWORD:-local_dev_only}" psql -h db -U app_user -d app_db \
  -f "$ROOT/application/flyway/profile/V1__init_profile_consolidated.sql" 2>/dev/null \
  && echo "  V1__init_profile_consolidated applied" || echo "  V1__init_profile_consolidated already applied — skipping"

# ── Profile test seed ─────────────────────────────────────────────────────────
# Insert the canonical test admin/profile rows so QuarkusTest suites for health,
# wealth, and household can resolve profile_id FK constraints immediately.
echo "==> Applying profile test seed..."
PGPASSWORD="${DB_PASSWORD:-local_dev_only}" psql -h db -U app_user -d app_db \
  -f "$ROOT/application/flyway/test-seed/profile/R__seed_profile_test_data.sql" 2>/dev/null \
  && echo "  Profile test seed applied" || echo "  Could not apply profile test seed — skipping"

# ── Shell aliases: load on every terminal open ───────────────────────────────
if ! grep -q "dev-aliases.sh" ~/.bashrc 2>/dev/null; then
  cat >> ~/.bashrc << EOF

# Suchika dev aliases — type help-dev for usage
export ROOT="$ROOT"
. "$ROOT/scripts/dev-aliases.sh"
EOF
  echo "  Added dev-aliases to ~/.bashrc"
fi

echo ""
echo "  Setup complete!"
echo "  Open a new terminal, then type: help-dev"
echo "  Start profile service first: dp"
echo "  Start all services at once:   da"
