#Requires -Version 5.1
# Ultimate pre-commit build verification for Suchika.
# Builds each domain one by one in dependency order, checks frontend lint/format/tests/build,
# runs ArchUnit, and optionally runs SonarQube analysis.
#
# Usage:
#   .\scripts\build-local.ps1
#   .\scripts\build-local.ps1 -SkipSonar
#   .\scripts\build-local.ps1 -SkipFrontend
param(
    [switch]$SkipSonar,
    [switch]$SkipFrontend,
    [switch]$FrontendOnly
)

if ($FrontendOnly) {
    $SkipSonar = $true
}

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Step($msg) { Write-Host "" ; Write-Host "==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "  [!]  $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "  [X]  $msg" -ForegroundColor Red; exit 1 }

function Elapsed($since) {
    $s = [int]([datetime]::Now - $since).TotalSeconds
    if ($s -ge 60) {
        "{0}m{1:D2}s" -f [int]($s / 60), ($s % 60)
    } else {
        "${s}s"
    }
}

function RunGradle($label, [string[]]$tasks) {
    $t = Get-Date
    Step $label
    & .\gradlew.bat $tasks
    if ($LASTEXITCODE -ne 0) { Fail "$label failed" }
    OK "$label  ($(Elapsed $t))"
}

$totalStart = Get-Date

if (-not $FrontendOnly) {

# --- 0. Pre-flight: check migration files are not inside adapters ---
Step "Pre-flight: migration file location check"
$misplacedMigrations = & git ls-files -- "application/domain/*/adapters" |
    Where-Object { $_ -match "db[/\\]migration" }
if ($misplacedMigrations) {
    $misplacedMigrations | ForEach-Object { Write-Host "  Misplaced: $_" -ForegroundColor Red }
    Fail "Flyway migration scripts found inside adapters/ -- move them to application/flyway/<domain>/"
}
OK "Migration locations clean"

# --- 1. Shared: AppLogger, exception hierarchy, ArchUnit fitness functions ---
RunGradle "shared (ArchUnit + exception hierarchy)" @(":shared:build", ":shared:test")

# --- 2. Profile: MUST run first; every other domain FKs into profile.profile ---
RunGradle "profile / domain layer" @(
    ":application:domain:profile:domain:build",
    ":application:domain:profile:domain:test"
)
RunGradle "profile / ports layer" @(
    ":application:domain:profile:ports:build",
    ":application:domain:profile:ports:test"
)
RunGradle "profile / adapters layer" @(
    ":application:domain:profile:adapters:build",
    ":application:domain:profile:adapters:test"
)

# --- 3. Health domain ---
RunGradle "health / domain layer" @(
    ":application:domain:health:domain:build",
    ":application:domain:health:domain:test"
)
RunGradle "health / ports layer" @(
    ":application:domain:health:ports:build",
    ":application:domain:health:ports:test"
)
RunGradle "health / adapters layer" @(
    ":application:domain:health:adapters:build",
    ":application:domain:health:adapters:test"
)

# --- 4. Household domain ---
RunGradle "household / domain layer" @(
    ":application:domain:household:domain:build",
    ":application:domain:household:domain:test"
)
RunGradle "household / ports layer" @(
    ":application:domain:household:ports:build",
    ":application:domain:household:ports:test"
)
RunGradle "household / adapters layer" @(
    ":application:domain:household:adapters:build",
    ":application:domain:household:adapters:test"
)

# --- 5. Wealth domain ---
RunGradle "wealth / domain layer" @(
    ":application:domain:wealth:domain:build",
    ":application:domain:wealth:domain:test"
)
RunGradle "wealth / ports layer" @(
    ":application:domain:wealth:ports:build",
    ":application:domain:wealth:ports:test"
)
RunGradle "wealth / adapters layer" @(
    ":application:domain:wealth:adapters:build",
    ":application:domain:wealth:adapters:test"
)

# --- 6. Web-gateway: BFF aggregator, no direct DB ---
RunGradle "web-gateway (BFF aggregator)" @(
    ":application:web-gateway:build",
    ":application:web-gateway:test"
)

} # end if (-not $FrontendOnly)

# --- 7. Frontend ---
if (-not $SkipFrontend) {
    Set-Location "$root\web"

    Step "Frontend: install dependencies"
    $t = Get-Date
    & npm ci --prefer-offline
    if ($LASTEXITCODE -ne 0) {
        Warn "npm ci failed (package-lock.json may be stale) -- falling back to npm install"
        & npm install
        if ($LASTEXITCODE -ne 0) { Fail "npm install failed" }
    }
    OK "Dependencies ready  ($(Elapsed $t))"

    Step "Frontend: generate API types from OpenAPI contract"
    $t = Get-Date
    & npm run generate:api
    if ($LASTEXITCODE -ne 0) {
        Warn "API generation skipped -- contract YAML not yet changed or gateway not built"
    } else {
        OK "API types generated  ($(Elapsed $t))"
    }

    Step "Frontend: ESLint"
    $t = Get-Date
    & npm run lint
    if ($LASTEXITCODE -ne 0) { Fail "ESLint errors -- fix before committing" }
    OK "Lint clean  ($(Elapsed $t))"

    Step "Frontend: Prettier format check"
    $t = Get-Date
    & npm run format:check
    if ($LASTEXITCODE -ne 0) {
        Fail "Formatting violations -- run 'npm run format' in web/, re-stage changed files, then re-run"
    }
    OK "Formatting clean  ($(Elapsed $t))"

    Step "Frontend: unit tests"
    $t = Get-Date
    cmd /c "npm run test:ci" 2>&1
    if ($LASTEXITCODE -ne 0) { Fail "Frontend tests failed" }
    OK "Tests passed  ($(Elapsed $t))"

    Step "Frontend: production build"
    $t = Get-Date
    & npm run build
    if ($LASTEXITCODE -ne 0) { Fail "Production build failed" }
    OK "Build OK  ($(Elapsed $t))"

    Set-Location $root
}

# --- 8. SonarQube analysis ---
if (-not $SkipSonar) {
    Step "SonarQube analysis"
    if (-not $env:GRADLE_USER_HOME) {
        $env:GRADLE_USER_HOME = "$env:USERPROFILE\.gradle"
    }

    $sonarUp = $false
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:9000/api/system/status" `
            -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        $sonarUp = ($r.StatusCode -eq 200)
    } catch {
        $sonarUp = $false
    }

    if (-not $sonarUp) {
        Warn "SonarQube not running at http://localhost:9000 -- skipping"
        Warn "Start it:  sonarqube\bin\windows-x86-64\StartSonar.bat"
        Warn "Then run:  sonar-scanner  (from the repo root)"
    } else {
        $scanner = Get-Command sonar-scanner -ErrorAction SilentlyContinue
        if (-not $scanner) {
            Warn "sonar-scanner not in PATH -- skipping"
            Warn "Install:  npm install -g sonar-scanner"
        } else {
            $t = Get-Date
            & sonar-scanner
            if ($LASTEXITCODE -eq 0) {
                OK "SonarQube analysis complete  ($(Elapsed $t))"
                Write-Host "     Dashboard: http://localhost:9000/dashboard?id=suchika" -ForegroundColor DarkGray
            } else {
                Warn "SonarQube reported issues -- check http://localhost:9000 (non-fatal)"
            }
        }
    }
}

# --- Summary ---
$s = [int]([datetime]::Now - $totalStart).TotalSeconds
Write-Host ""
Write-Host ("  All checks passed in {0}m{1:D2}s -- safe to commit." -f [int]($s / 60), ($s % 60)) -ForegroundColor Green
