#!/bin/bash
# dev-aliases.sh — Bash equivalent of dev-aliases.ps1 for Codespaces / Linux.
# Source this file (do not execute it):
#
#   . ./scripts/dev-aliases.sh
#
# All functions start services in background and write to ~/.suchika/logs/<svc>.log.
# Watch live output: lnav-dev   or   tail -f ~/.suchika/logs/profile.log
#
# Note: dev-service.ps1 opens new terminal windows (Windows only). In Codespaces,
# open a new VS Code terminal manually for each service, or let da() run them all
# in the background and watch with lnav-dev.

ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null || echo "$PWD")"
LOG_DIR="$HOME/.suchika/logs"
mkdir -p "$LOG_DIR"

# ── Internal helper ───────────────────────────────────────────────────────────

_dev_svc() {
  local svc="$1" task="$2"
  local log="$LOG_DIR/$svc.log"
  echo "==> Starting $svc... (log: $log)"
  (cd "$ROOT" && ./gradlew "$task" >> "$log" 2>&1) &
  disown
  echo "  PID $! started for $svc"
}

# ── Dev mode (background) ─────────────────────────────────────────────────────

dev-profile()   { _dev_svc profile   ":application:domain:profile:adapters:quarkusDev"; }
dev-wealth()    { _dev_svc wealth    ":application:domain:wealth:adapters:quarkusDev"; }
dev-health()    { _dev_svc health    ":application:domain:health:adapters:quarkusDev"; }
dev-household() { _dev_svc household ":application:domain:household:adapters:quarkusDev"; }
dev-gateway()   { _dev_svc gateway   ":application:web-gateway:quarkusDev"; }
dev-web()       {
  local log="$LOG_DIR/web.log"
  echo "==> Starting frontend... (log: $log)"
  (cd "$ROOT/web" && npm start >> "$log" 2>&1) &
  disown
  echo "  PID $! started for web"
}

alias dp='dev-profile'
alias dw='dev-wealth'
alias dh='dev-health'
alias dho='dev-household'
alias dg='dev-gateway'
alias dwb='dev-web'

dev-all() {
  echo "==> Starting all services in dependency order..."
  dev-profile
  echo "  Waiting for profile (8081)..."
  timeout 120 bash -c 'until curl -sf http://localhost:8081/q/openapi >/dev/null 2>&1; do sleep 3; done' \
    && echo "  Profile ready" || echo "  Timeout — check log: $LOG_DIR/profile.log"
  dev-wealth; dev-health; dev-household
  sleep 5
  dev-gateway
  echo "  Waiting for gateway (8080)..."
  timeout 60 bash -c 'until curl -sf http://localhost:8080/q/openapi >/dev/null 2>&1; do sleep 3; done' \
    && echo "  Gateway ready" || echo "  Timeout — check log: $LOG_DIR/gateway.log"
  dev-web
  echo ""
  echo "  All services started. Watch logs: lnav-dev"
  echo "  Frontend: http://localhost:3000"
}
alias da='dev-all'

# ── Build ─────────────────────────────────────────────────────────────────────

build-profile()   { cd "$ROOT" && ./gradlew :application:domain:profile:adapters:build --no-build-cache; }
build-wealth()    { cd "$ROOT" && ./gradlew :application:domain:wealth:adapters:build --no-build-cache; }
build-health()    { cd "$ROOT" && ./gradlew :application:domain:health:adapters:build --no-build-cache; }
build-household() { cd "$ROOT" && ./gradlew :application:domain:household:adapters:build --no-build-cache; }
build-gateway()   { cd "$ROOT" && ./gradlew :application:web-gateway:build --no-build-cache; }
build-web()       { cd "$ROOT/web" && npm run build; }
build-shared()    { cd "$ROOT" && ./gradlew :shared:build --no-build-cache; }
build-all()       { cd "$ROOT" && ./gradlew build --no-build-cache && cd web && npm run build; }
build-verify()    {
  if command -v pwsh >/dev/null 2>&1; then
    pwsh "$ROOT/scripts/build-local.ps1" -SkipSonar
  else
    bash "$ROOT/scripts/build-local.sh"
  fi
}

alias bp='build-profile'
alias bw='build-wealth'
alias bh='build-health'
alias bho='build-household'
alias bg='build-gateway'
alias bwb='build-web'
alias ba='build-all'
alias bv='build-verify'

# ── Test ──────────────────────────────────────────────────────────────────────

test-profile()   { cd "$ROOT" && ./gradlew :application:domain:profile:domain:test :application:domain:profile:adapters:test; }
test-wealth()    { cd "$ROOT" && ./gradlew :application:domain:wealth:domain:test :application:domain:wealth:adapters:test; }
test-health()    { cd "$ROOT" && ./gradlew :application:domain:health:domain:test :application:domain:health:adapters:test; }
test-household() { cd "$ROOT" && ./gradlew :application:domain:household:domain:test :application:domain:household:adapters:test; }
test-gateway()   { cd "$ROOT" && ./gradlew :application:web-gateway:test; }
test-shared()    { cd "$ROOT" && ./gradlew :shared:test; }
test-web()       { cd "$ROOT/web" && npm run test:ci; }
test-all()       { cd "$ROOT" && ./gradlew test; }

alias tp='test-profile'
alias tw='test-wealth'
alias tsa='test-all'

# ── Quality ───────────────────────────────────────────────────────────────────

sonar-scan() {
  if command -v pwsh >/dev/null 2>&1; then
    pwsh "$ROOT/scripts/sonar-scan.ps1"
  else
    echo "sonar-scan requires pwsh (PowerShell Core). Install: sudo apt-get install -y powershell"
  fi
}
generate-api() { cd "$ROOT/web" && npm run generate:api; }

alias ss='sonar-scan'
alias gapi='generate-api'

# ── Status / Control ─────────────────────────────────────────────────────────

status() { bash "$ROOT/scripts/health-check.sh"; }

stop-all() {
  echo "Stopping all services..."
  for port in 3000 8080 8081 8082 8083 8084; do
    local pids
    pids=$(lsof -ti tcp:"$port" 2>/dev/null)
    if [ -n "$pids" ]; then
      echo "$pids" | xargs kill 2>/dev/null
      echo "  Killed port $port"
    fi
  done
}
alias sa='stop-all'

check() { bash "$ROOT/scripts/check-prerequisites.sh"; }

# ── Database ─────────────────────────────────────────────────────────────────

db-shell() {
  local host="${PGHOST:-localhost}"
  # Same fallback chain as db-reset.ps1: PGPASSWORD, else POSTGRES_PASSWORD, else the
  # 00_bootstrap.sql / docker-compose default. Set PGPASSWORD yourself if your local
  # postgres superuser password differs (it will, on a natively-installed Postgres).
  local admin_pw="${PGPASSWORD:-${POSTGRES_PASSWORD:-local_dev_only}}"
  if [[ "$1" == "-AsAdmin" ]]; then
    PGPASSWORD="$admin_pw" psql -h "$host" -U postgres app_db
  else
    PGPASSWORD="${DB_PASSWORD:-local_dev_only}" psql -h "$host" -U app_user app_db
  fi
}

db-reset() {
  if [[ "$1" != "-Force" ]]; then
    read -rp "This will DROP and recreate app_db — all data lost. Continue? [y/N] " confirm
    [[ "$confirm" == "y" ]] || { echo "Aborted."; return 1; }
  fi
  local host="${PGHOST:-localhost}"
  # Same fallback chain as db-reset.ps1: PGPASSWORD, else POSTGRES_PASSWORD, else the
  # 00_bootstrap.sql / docker-compose default.
  local admin_pw="${PGPASSWORD:-${POSTGRES_PASSWORD:-local_dev_only}}"
  PGPASSWORD="$admin_pw" psql -h "$host" -U postgres \
    -c "DROP DATABASE IF EXISTS app_db;" \
    -c "CREATE DATABASE app_db;"
  PGPASSWORD="$admin_pw" psql -h "$host" -U postgres -d app_db \
    -f "$ROOT/application/flyway/00_bootstrap.sql"
  echo "  Done. Start profile service to run Flyway migrations."
}

# ── Logs ─────────────────────────────────────────────────────────────────────

lnav-dev() {
  local targets=()
  if [ -z "$1" ]; then
    while IFS= read -r f; do targets+=("$f"); done < <(ls "$LOG_DIR"/*.log 2>/dev/null)
  else
    IFS=',' read -ra svcs <<< "$1"
    for s in "${svcs[@]}"; do targets+=("$LOG_DIR/$s.log"); done
  fi
  # Create files that don't exist yet so lnav doesn't error
  for f in "${targets[@]}"; do touch "$f" 2>/dev/null; done
  lnav "${targets[@]}"
}

logs() {
  local svc="$1" logdir="$ROOT/.temp/logs"
  if [ -n "$svc" ]; then
    local latest; latest=$(ls -t "$logdir/$svc/"*.log 2>/dev/null | head -1)
    [ -n "$latest" ] && tail -50 "$latest" || echo "No logs for $svc in $logdir"
  else
    echo "Latest build/test logs:"
    for d in "$logdir"/*/; do
      local latest; latest=$(ls -t "$d"*.log 2>/dev/null | head -1)
      [ -n "$latest" ] && echo "  $(basename "$d"): $latest"
    done
  fi
}

# ── Cleanup ───────────────────────────────────────────────────────────────────

clean-builds() {
  cd "$ROOT" && ./gradlew clean
  rm -rf web/build .temp
  echo "  Build outputs removed."
}

# ── Help ─────────────────────────────────────────────────────────────────────

help-dev() {
cat << 'EOF'
  Suchika dev aliases (bash) — source: . ./scripts/dev-aliases.sh

  BUILD             SERVICES (bg)           TESTS
  ─────             ─────────────           ─────
  bp build-profile  dp  dev-profile  :8081  tp  test-profile
  bw build-wealth   dw  dev-wealth   :8082  tw  test-wealth
  bh build-health   dh  dev-health   :8083  tsa test-all
  bho build-hshld   dho dev-household:8084  test-health/-household (no alias)
  bg build-gateway  dg  dev-gateway  :8080  test-gateway/-shared    (no alias)
  bwb build-web     dwb dev-web      :3000  test-web                (no alias)
  ba build-all      da  dev-all             QUALITY
  bv build-verify                           ─────────
  build-shared (no alias)                   ss   sonar-scan
                                            gapi generate-api
  STATUS / CONTROL      DATABASE       LOGS
  ────────────────      ────────       ────
  status  health        db-shell       logs [svc]
  sa      stop-all      db-reset       lnav-dev [svcs]
  check   prereqs       db-shell -AsAdmin

  NOTE: sonar-scan needs pwsh + a local SonarQube server -- not run in Codespaces
        (see CLAUDE.md). db-reset/db-shell -AsAdmin need the REAL postgres
        superuser password if it isn't 'local_dev_only' on this machine --
        export PGPASSWORD before calling them if so.
EOF
}
