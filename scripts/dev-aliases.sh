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

# Single source of truth for ports/schemas/gradle wiring (scripts/services.json)
# and the PID-file service registry -- both shared with dev-service.ps1 /
# stop-all.ps1 / health-check.ps1 on the Windows side.
. "$ROOT/scripts/config.sh"
. "$ROOT/scripts/service-registry.sh"
LOG_DIR="$SUCHIKA_LOG_DIR"

# ── Internal helper ───────────────────────────────────────────────────────────

_dev_svc() {
  local svc="$1"
  local task port log
  task=$(suchika_svc_field "$svc" devTask)
  port=$(suchika_svc_field "$svc" port)
  log="$LOG_DIR/$svc.log"
  echo "==> Starting $svc... (log: $log)"
  (cd "$ROOT" && ./gradlew "$task" >> "$log" 2>&1) &
  disown
  echo "  PID $! (shell wrapper) started for $svc -- resolving real server PID in background"
  # The backgrounded PID above is the gradlew wrapper, not the eventual java.exe
  # process Gradle forks. Register the real one once the port actually binds
  # (see service-registry.sh) so stop-all/status can act on the right process.
  suchika_register_service_async "$svc" "$port"
}

# ── Dev mode (background) ─────────────────────────────────────────────────────

dev-profile()   { _dev_svc profile; }
dev-wealth()    { _dev_svc wealth; }
dev-health()    { _dev_svc health; }
dev-household() { _dev_svc household; }
dev-gateway()   { _dev_svc gateway; }
dev-web()       {
  local port log
  port=$(suchika_svc_field web port)
  log="$LOG_DIR/web.log"
  echo "==> Starting frontend... (log: $log)"
  (cd "$ROOT/web" && npm start >> "$log" 2>&1) &
  disown
  echo "  PID $! (shell wrapper) started for web -- resolving real server PID in background"
  suchika_register_service_async web "$port"
}

alias dp='dev-profile'
alias dw='dev-wealth'
alias dh='dev-health'
alias dho='dev-household'
alias dg='dev-gateway'
alias dwb='dev-web'

dev-all() {
  local profile_port gateway_port
  profile_port=$(suchika_svc_field profile port)
  gateway_port=$(suchika_svc_field gateway port)
  echo "==> Starting all services in dependency order..."
  dev-profile
  echo "  Waiting for profile ($profile_port)..."
  # Real /q/health (quarkus-smallrye-health), not /q/openapi -- a 5xx here now
  # correctly counts as "not ready" instead of being treated as UP.
  timeout 120 bash -c "until curl -sf http://localhost:$profile_port/q/health >/dev/null 2>&1; do sleep 3; done" \
    && echo "  Profile ready" || echo "  Timeout — check log: $LOG_DIR/profile.log"
  dev-wealth; dev-health; dev-household
  sleep 5
  dev-gateway
  echo "  Waiting for gateway ($gateway_port)..."
  timeout 60 bash -c "until curl -sf http://localhost:$gateway_port/q/health >/dev/null 2>&1; do sleep 3; done" \
    && echo "  Gateway ready" || echo "  Timeout — check log: $LOG_DIR/gateway.log"
  dev-web
  echo ""
  echo "  All services started. Watch logs: lnav-dev"
  echo "  Frontend: http://localhost:$(suchika_svc_field web port)"
}
alias da='dev-all'

# ── Headless run (no GUI windows -- "I just want it running") ─────────────────
# On bash dev-all above is already headless (backgrounded, no window concept in a
# Codespaces/Linux terminal), so these are thin wrappers -- kept as their own named
# commands purely for surface parity with Windows's run-local.ps1/stop-local.ps1,
# where run-local genuinely needed new logic (Windows normally opens a GUI window
# per service). See scripts/run-local.sh / stop-local.sh.

run-local()  { bash "$ROOT/scripts/run-local.sh" "$@"; }
stop-local() { bash "$ROOT/scripts/stop-local.sh" "$@"; }
# "stopl", not "sl" -- kept consistent with the PowerShell side, where "sl" is
# PowerShell's own built-in Set-Location alias and can't be reused.
alias rl='run-local'
alias stopl='stop-local'

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
  # Optional $1 scopes the same registry-first/port-fallback kill loop to one
  # named service instead of all of them (used by stop-local <svc>). Omitting
  # it keeps the existing "stop everything" behavior as the default.
  local only="${1:-}"
  if [ -n "$only" ]; then
    echo "Stopping $only..."
  else
    echo "Stopping all services..."
  fi
  local svc port pid
  for svc in $(suchika_service_names); do
    if [ -n "$only" ] && [ "$svc" != "$only" ]; then
      continue
    fi
    port=$(suchika_svc_field "$svc" port)
    # PID registry first (see service-registry.sh) -- falls back to today's
    # port-based kill only if no valid registered PID exists.
    pid=$(suchika_get_running_pid "$svc" 2>/dev/null)
    if [ -n "$pid" ]; then
      kill "$pid" 2>/dev/null
      suchika_remove_service_pid "$svc"
      echo "  Killed $svc (PID $pid, port $port)  [registry]"
      continue
    fi
    local pids
    pids=$(lsof -ti tcp:"$port" 2>/dev/null)
    if [ -n "$pids" ]; then
      echo "$pids" | xargs kill 2>/dev/null
      echo "  Killed $svc (port $port)  [port fallback]"
    fi
  done
}
alias sa='stop-all'

check() { bash "$ROOT/scripts/check-prerequisites.sh"; }

# ── Database ─────────────────────────────────────────────────────────────────

db-shell() {
  local host="${PGHOST:-localhost}"
  # Same fallback chain as db-reset.ps1: PGPASSWORD, else POSTGRES_PASSWORD, else the
  # scripts/services.json default (mirrors 00_bootstrap.sql / docker-compose). Set
  # PGPASSWORD yourself if your local postgres superuser password differs (it will,
  # on a natively-installed Postgres).
  local admin_pw="${PGPASSWORD:-${POSTGRES_PASSWORD:-$SUCHIKA_DB_PASSWORD_FALLBACK}}"
  if [[ "$1" == "-AsAdmin" ]]; then
    PGPASSWORD="$admin_pw" psql -h "$host" -U postgres app_db
  else
    PGPASSWORD="${DB_PASSWORD:-$SUCHIKA_DB_PASSWORD_FALLBACK}" psql -h "$host" -U app_user app_db
  fi
}

db-reset() {
  if [[ "$1" != "-Force" ]]; then
    read -rp "This will DROP and recreate app_db — all data lost. Continue? [y/N] " confirm
    [[ "$confirm" == "y" ]] || { echo "Aborted."; return 1; }
  fi
  local host="${PGHOST:-localhost}"
  # Same fallback chain as db-reset.ps1: PGPASSWORD, else POSTGRES_PASSWORD, else the
  # scripts/services.json default.
  local admin_pw="${PGPASSWORD:-${POSTGRES_PASSWORD:-$SUCHIKA_DB_PASSWORD_FALLBACK}}"
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

  HEADLESS RUN (rl/stopl) -- same as da/sa on bash (already headless); the pair
  exists for command-surface parity with Windows, where run-local genuinely differs
  from dev-all (no GUI window at all vs. one per service):
  rl    run-local  -- start everything headlessly, wait for real /q/health
  stopl stop-local -- stop everything run-local started

  NOTE: sonar-scan needs pwsh + a local SonarQube server -- not run in Codespaces
        (see CLAUDE.md). db-reset/db-shell -AsAdmin need the REAL postgres
        superuser password if it isn't 'local_dev_only' on this machine --
        export PGPASSWORD before calling them if so.
EOF
}
