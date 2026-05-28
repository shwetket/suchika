#!/usr/bin/env bash
set -euo pipefail

if git ls-files --error-unmatch '*/adapters/**/db/migration/*' >/dev/null 2>&1; then
  echo "ERROR: Flyway scripts found in adapters. Move them to application/flyway/*" >&2
  exit 1
fi

echo "OK: migrations location clean"
