#!/bin/bash
# setup.sh
# Runs on first codespace creation (postCreateCommand).
# Assumes download-deps.sh already ran during prebuild.
# Safe to re-run.

set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Setting up Suchika development environment..."

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
PGPASSWORD=local_dev_only psql -h db -U postgres -c "CREATE DATABASE app_db;" 2>/dev/null \
  && echo "  Created app_db" \
  || echo "  app_db already exists — skipping"

PGPASSWORD=local_dev_only psql -h db -U postgres -d app_db \
  -f "$ROOT/application/flyway/00_bootstrap.sql" 2>/dev/null \
  && echo "  Bootstrap SQL applied" \
  || echo "  Bootstrap already applied — skipping"

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
