# Mirrors the CI pipeline exactly — run before committing to catch failures locally.
# Usage: .\scripts\build-local.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "v $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "x $msg" -ForegroundColor Red; exit 1 }

$start = Get-Date

# ── 1. Migration location check ──────────────────────────────────────────────
Step "Checking migration file locations..."
& bash scripts/check-migrations-location.sh
if ($LASTEXITCODE -ne 0) { Fail "Migration location check failed" }
OK "Migration location check passed"

# ── 2. Backend tests (includes ArchUnit) ─────────────────────────────────────
Step "Running backend tests (includes ArchUnit)..."
& .\gradlew.bat test --continuous=false
if ($LASTEXITCODE -ne 0) { Fail "Backend tests failed" }
OK "Backend tests passed"

# ── 3. Frontend ───────────────────────────────────────────────────────────────
Set-Location "$root\web"

Step "Installing frontend dependencies..."
& npm install
if ($LASTEXITCODE -ne 0) { Fail "npm install failed" }
OK "Dependencies installed"

Step "Generating API types from OpenAPI contract..."
& npm run generate:api
if ($LASTEXITCODE -ne 0) { Fail "API generation failed" }
OK "API types generated"

Step "Linting frontend..."
& npm run lint
if ($LASTEXITCODE -ne 0) { Fail "Lint failed" }
OK "Lint passed"

Step "Fixing frontend formatting (auto-fix before commit)..."
& npm run format
if ($LASTEXITCODE -ne 0) { Fail "Formatting failed" }
OK "Formatting applied"

Step "Running frontend unit tests..."
# PowerShell 5.1 wraps native process stderr as ErrorRecord — use cmd /c to avoid false failures
cmd /c "npm run test:ci" 2>&1
if ($LASTEXITCODE -ne 0) { Fail "Frontend tests failed" }
OK "Frontend tests passed"

Step "Building React frontend..."
& npm run build
if ($LASTEXITCODE -ne 0) { Fail "Frontend build failed" }
OK "Frontend build passed"

Set-Location $root

$elapsed = [int]((Get-Date) - $start).TotalSeconds
Write-Host "`nAll checks passed in ${elapsed}s -- safe to commit." -ForegroundColor Green
