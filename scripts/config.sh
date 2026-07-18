#!/usr/bin/env bash
# Single source of truth loader for Suchika bash scripts.
# Reads scripts/services.json using grep -oP (Perl-compatible regex). No jq
# dependency: jq is not guaranteed present (confirmed absent from this repo's
# own dev environment and not installed by .devcontainer/setup.sh), and pulling
# it in as a new tool wasn't justified just to read six flat, single-line JSON
# objects -- grep -oP is already relied on elsewhere in this repo's bash tooling
# and ships with the GNU grep on both Codespaces (Debian) and Git Bash (MSYS).
# Source this file; do not execute it directly.
#
# Usage (from another script in scripts/):
#   . "$(dirname "${BASH_SOURCE[0]}")/config.sh"
#   suchika_svc_field profile port         # -> 8081
#   suchika_svc_field profile devTask      # -> :application:domain:profile:adapters:quarkusDev
#   suchika_db_field passwordFallback      # -> local_dev_only

SUCHIKA_CONFIG_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUCHIKA_SERVICES_JSON="$SUCHIKA_CONFIG_DIR/services.json"

if [[ ! -f "$SUCHIKA_SERVICES_JSON" ]]; then
  echo "scripts/services.json not found at $SUCHIKA_SERVICES_JSON -- single source of truth for ports/schemas is missing." >&2
  return 1 2>/dev/null || exit 1
fi

# suchika_svc_field <service> <field> -- prints one field from the single-line
# service object whose "name" matches <service>. Numeric fields (port,
# startOrder) are unquoted in JSON; everything else is a quoted string.
suchika_svc_field() {
  local svc="$1" field="$2" line
  line=$(grep "\"name\": *\"$svc\"" "$SUCHIKA_SERVICES_JSON") || return 1
  case "$field" in
    port|startOrder)
      echo "$line" | grep -oP "\"$field\":\s*\K[0-9]+" ;;
    *)
      echo "$line" | grep -oP "\"$field\":\s*\"\K[^\"]*" ;;
  esac
}

# suchika_db_field <field> -- same, for the top-level "database" object.
suchika_db_field() {
  local field="$1" line
  line=$(grep '"database":' "$SUCHIKA_SERVICES_JSON") || return 1
  case "$field" in
    port)
      echo "$line" | grep -oP "\"$field\":\s*\K[0-9]+" ;;
    *)
      echo "$line" | grep -oP "\"$field\":\s*\"\K[^\"]*" ;;
  esac
}

# suchika_version_floor <field> -- same, for the top-level "versionFloors" object.
suchika_version_floor() {
  local field="$1" line
  line=$(grep '"versionFloors":' "$SUCHIKA_SERVICES_JSON") || return 1
  echo "$line" | grep -oP "\"$field\":\s*\K[0-9]+"
}

# All service names in file order (services.json is already written in startOrder).
suchika_service_names() {
  grep -oP '"name":\s*"\K[^"]+' "$SUCHIKA_SERVICES_JSON"
}

SUCHIKA_DB_PASSWORD_FALLBACK="$(suchika_db_field passwordFallback)"
SUCHIKA_PID_DIR="$HOME/.suchika/run"
SUCHIKA_LOG_DIR="$HOME/.suchika/logs"
mkdir -p "$SUCHIKA_PID_DIR" "$SUCHIKA_LOG_DIR"

# suchika_wait_healthy <service> [timeoutSec] -- polls a service's /q/health (or,
# for the frontend, its plain root) until it responds HTTP 200, or the timeout
# elapses. Returns 0 (ready) / 1 (timed out). Shared by dev-all and run-local.sh.
suchika_wait_healthy() {
  local svc="$1" timeout="${2:-90}" port path url waited=0
  port=$(suchika_svc_field "$svc" port)
  path=$(suchika_svc_field "$svc" healthPath)
  # 127.0.0.1, not "localhost" -- mirrors the same fix in config.ps1's
  # Get-SuchikaHealthUrl (confirmed there that "localhost" can resolve to ::1
  # first and hang against an IPv4-only bind); kept consistent here even though
  # curl generally fails fast on refused IPv6 connections on Linux.
  url="http://127.0.0.1:$port$path"
  while (( waited < timeout )); do
    if curl -sf --max-time 2 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    (( waited += 2 ))
  done
  return 1
}
