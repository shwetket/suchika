#!/usr/bin/env bash
# Verifies every .ps1 file under scripts/ is saved as UTF-8 WITH a byte-order
# mark. Windows PowerShell 5.1 (powershell.exe, not pwsh.exe) decodes a
# BOM-less .ps1 using the system ANSI codepage, not UTF-8 -- any non-ASCII
# character sitting inside a string literal (em dash, arrows, etc.) gets
# mis-decoded and can silently corrupt string/quote parsing for the rest of
# the file. See documents/SCRIPTS.md "Encoding note" for the full writeup.
# Mirrors check-migrations-location.sh's pattern; called from .husky/pre-commit.
# Usage: bash scripts/check-ps1-bom.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

missing=()
while IFS= read -r -d '' f; do
  first3=$(head -c 3 "$f" | od -An -tx1 | tr -d ' \n')
  if [[ "$first3" != "efbbbf" ]]; then
    missing+=("$f")
  fi
done < <(find "$SCRIPT_DIR" -maxdepth 1 -name '*.ps1' -print0)

if (( ${#missing[@]} > 0 )); then
  echo "ERROR: the following scripts/*.ps1 files are missing a UTF-8 BOM:" >&2
  for f in "${missing[@]}"; do echo "  - $f" >&2; done
  echo "Re-save with UTF-8 BOM encoding, e.g. in PowerShell:" >&2
  echo "  \$c = Get-Content -Raw -Path <file>; [IO.File]::WriteAllText('<file>', \$c, (New-Object Text.UTF8Encoding(\$true)))" >&2
  exit 1
fi

echo "OK: all scripts/*.ps1 files have a UTF-8 BOM"
