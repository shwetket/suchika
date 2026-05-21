#Requires -Version 5.1
# First-time developer setup for Suchika.
# Run once on a fresh clone or after clean-all.
# Usage: .\scripts\setup-dev.ps1
param([switch]$SkipDb)

$ErrorActionPreference = 'Continue'
$root    = Split-Path -Parent $PSScriptRoot
$envSrc  = Join-Path $root 'infrastructure\local\.env.template'
$envDest = Join-Path $root 'application\finance\.env'
$boot    = Join-Path $root 'application\flyway\00_bootstrap.sql'
$psqlExe = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "  [!]   $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "  [X]   $msg" -ForegroundColor Red }

function FindPsql {
    $fromPath = (Get-Command psql -ErrorAction SilentlyContinue)?.Source
    if ($fromPath) { return $fromPath }
    if (Test-Path $psqlExe) { return $psqlExe }
    $found = Get-ChildItem 'C:\Program Files\PostgreSQL' -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
             Sort-Object FullName -Descending | Select-Object -First 1
    return $found?.FullName
}

Write-Host "`n===  Suchika First-Time Setup  ===" -ForegroundColor Cyan

# ── 1. Prerequisites check ────────────────────────────────────────────────────
Step "1/5  Checking prerequisites"
& (Join-Path $root 'scripts\check-prerequisites.ps1')

# ── 2. .env file ──────────────────────────────────────────────────────────────
Step "2/5  Environment file"
if (Test-Path $envDest) {
    OK ".env already exists at $envDest"
} else {
    if (Test-Path $envSrc) {
        Copy-Item $envSrc $envDest
        OK ".env copied from template"
        Warn "Edit $envDest and set DB_PASSWORD before starting any service."
    } else {
        Warn ".env template not found at $envSrc"
    }
}

# ── 3. Database setup ─────────────────────────────────────────────────────────
Step "3/5  Database"
if ($SkipDb) {
    Warn "Skipping DB setup (-SkipDb flag set)"
} else {
    $psql = FindPsql
    if (-not $psql) {
        Warn "psql not found — skipping DB setup. Install PostgreSQL and rerun."
    } else {
        # Ensure PostgreSQL is running
        $pgSvc = Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($pgSvc -and $pgSvc.Status -ne 'Running') {
            try { Start-Service $pgSvc.Name; OK "$($pgSvc.Name) started" } catch { Warn "Could not start PostgreSQL service: $_" }
        }

        # Check if app_db already exists
        $dbExists = (& $psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='app_db'" 2>&1) -eq '1'
        if ($dbExists) {
            OK "app_db already exists — skipping create+bootstrap"
            Warn "Run db-reset if you want a clean slate."
        } else {
            & $psql -U postgres -c 'CREATE DATABASE app_db;' 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                OK "app_db created"
                & $psql -U postgres -d app_db -f $boot 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    OK "Bootstrap SQL applied (schemas + app_user)"
                } else {
                    Fail "Bootstrap SQL failed — check $boot"
                }
            } else {
                Fail "CREATE DATABASE failed — check PostgreSQL is running and you have superuser access"
            }
        }
    }
}

# ── 4. Frontend dependencies ──────────────────────────────────────────────────
Step "4/5  Frontend dependencies (npm install)"
Push-Location (Join-Path $root 'web')
try {
    & npm install
    if ($LASTEXITCODE -eq 0) { OK "node_modules installed" } else { Warn "npm install failed" }
} finally {
    Pop-Location
}

# ── 5. Summary ────────────────────────────────────────────────────────────────
Step "5/5  Next steps"
Write-Host @"

  Activate dev aliases (do this once per terminal session):
    . .\scripts\dev-aliases.ps1

  Start developing:
    dev-profile          ← start profile service FIRST (others FK into it)
    dev-all              ← then start all remaining services + web
    status               ← check everything is up

  Need a clean DB?       db-reset
  Build for CI check?    build-verify
  Run SonarQube?         sonar-start  then  sonar-scan

"@ -ForegroundColor DarkGray
