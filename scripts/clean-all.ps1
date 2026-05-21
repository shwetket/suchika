#Requires -Version 5.1
# Nuclear clean: removes everything git doesn't track, INCLUDING node_modules and .gradle cache.
# SAFE: always excludes application/finance/.env so you don't lose DB credentials.
# Shows a dry-run preview before doing anything.
# Usage: .\scripts\clean-all.ps1  [-Force]
param([switch]$Force)

$root = Split-Path -Parent $PSScriptRoot

Write-Host "`n==> Nuclear clean (git clean -fdx)" -ForegroundColor Red
Write-Host "    This removes node_modules, .gradle cache, all build outputs." -ForegroundColor Yellow
Write-Host "    Your .env file is preserved." -ForegroundColor Yellow

Push-Location $root
try {
    Write-Host "`n--- Dry-run preview (files that WOULD be deleted) ---" -ForegroundColor Cyan
    & git clean -fdxn --exclude='application/finance/.env' --exclude='.env'
    Write-Host "--- End of preview ---`n" -ForegroundColor Cyan

    if (-not $Force) {
        $confirm = Read-Host "  Type 'clean' to proceed, anything else to abort"
        if ($confirm -ne 'clean') {
            Write-Host "  Aborted." -ForegroundColor Yellow
            return
        }
    }

    & git clean -fdx --exclude='application/finance/.env' --exclude='.env'
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n  [OK]  Repository cleaned." -ForegroundColor Green
        Write-Host "        Run: setup-dev   if this is a fresh clone, or" -ForegroundColor DarkGray
        Write-Host "             build-all   to rebuild." -ForegroundColor DarkGray
    } else {
        Write-Host "`n  [!]  git clean exited $LASTEXITCODE" -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}
