#!/usr/bin/env bash
# Headless "just run the app" mode -- bash counterpart to run-local.ps1.
#
# bash's dev-all (da, in dev-aliases.sh) already starts every service in the
# background writing to ~/.suchika/logs/<svc>.log and registers each one's real PID
# via the Phase 1 registry -- there's no GUI-window concept to bring to parity with
# on this side (a Codespaces/Linux terminal doesn't have one). So this script is a
# thin wrapper around the existing dev-all, not a reinvention: it exists mainly for
# command-surface parity with Windows (where run-local.ps1 genuinely needed new
# headless logic) and so `run-local`/`rl` reads the same on both platforms.
#
# Usage: bash scripts/run-local.sh
#        bash scripts/run-local.sh wealth   # start ONLY wealth, leave everything else alone
#
# Positional $1 targets exactly one entry from scripts/services.json (any of
# profile, wealth, health, household, gateway, web) -- reuses the same
# _dev_svc/dev-web start + PID-registry tracking already used by dev-all, just
# scoped to one service, then waits on its real /q/health via
# suchika_wait_healthy. Omitting $1 keeps the existing "start everything"
# behavior (dev-all) as the default -- unchanged.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/dev-aliases.sh"

SVC="${1:-}"
if [ -n "$SVC" ]; then
  # suchika_svc_field returns non-zero / empty on an unknown name -- fail fast
  # instead of silently doing nothing.
  if [ -z "$(suchika_svc_field "$SVC" port)" ]; then
    echo "Unknown service '$SVC' -- check scripts/services.json" >&2
    exit 1
  fi
  echo "==> run-local $SVC: starting only $SVC headlessly"
  if [ "$SVC" = "web" ]; then
    dev-web
  else
    _dev_svc "$SVC"
  fi
  echo "  Waiting for $SVC health..."
  if suchika_wait_healthy "$SVC" 180; then
    echo "  $SVC healthy. Other services untouched."
  else
    echo "  Timeout waiting for $SVC -- check log: $SUCHIKA_LOG_DIR/$SVC.log" >&2
    exit 1
  fi
  exit 0
fi

dev-all
