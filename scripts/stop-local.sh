#!/usr/bin/env bash
# Stops services started by run-local.sh. Thin wrapper around bash's stop-all (sa) --
# see stop-local.ps1 for the full reasoning: stop-all already does PID-registry-first,
# port-based-fallback kill for every service regardless of how it was started, so
# there's nothing headless-specific left to write here.
#
# Usage: bash scripts/stop-local.sh
#        bash scripts/stop-local.sh wealth   # stop ONLY wealth
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/dev-aliases.sh"
stop-all "$@"
