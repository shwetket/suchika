#Requires -Version 5.1
# Runs SonarQube analysis and opens the project dashboard.
# Requires: SonarQube running at http://localhost:9000 and sonar-scanner in PATH.
# Usage: .\scripts\sonar-scan.ps1

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "  [!]   $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "  [X]   $msg" -ForegroundColor Red }

# ── Preflight: SonarQube server running? ──────────────────────────────────────
Step "Checking SonarQube at http://localhost:9000"
$sonarUp = $false
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:9000/api/system/status' -UseBasicParsing -TimeoutSec 4 -ErrorAction Stop
    $sonarUp = ($r.StatusCode -eq 200)
} catch { }

if (-not $sonarUp) {
    Fail "SonarQube is not running."
    Write-Host "       Start it first:  sonar-start" -ForegroundColor Yellow
    exit 1
}
OK "SonarQube is UP"

# ── Preflight: sonar-scanner in PATH? ─────────────────────────────────────────
if (-not (Get-Command sonar-scanner -ErrorAction SilentlyContinue)) {
    Fail "sonar-scanner not found in PATH."
    Write-Host "       Install:  npm install -g sonar-scanner" -ForegroundColor Yellow
    exit 1
}

# ── Run analysis ──────────────────────────────────────────────────────────────
Step "Running sonar-scanner  (project: suchika)"

# Set GRADLE_USER_HOME so Sonar can find the compiled JARs declared in sonar-project.properties
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
}

Push-Location $root
try {
    $start = Get-Date
    & sonar-scanner
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

$elapsed = [int]([datetime]::Now - $start).TotalSeconds

if ($exit -eq 0) {
    OK "Analysis complete  ($($elapsed)s)"
    Write-Host "       Dashboard: http://localhost:9000/dashboard?id=suchika" -ForegroundColor DarkGray
    Start-Process 'http://localhost:9000/dashboard?id=suchika'
} else {
    Fail "sonar-scanner exited with code $exit  ($($elapsed)s)"
    Write-Host "       Check dashboard for details: http://localhost:9000/dashboard?id=suchika" -ForegroundColor Yellow
    Start-Process 'http://localhost:9000/dashboard?id=suchika'
    exit $exit
}
