#Requires -Version 5.1
# ============================================================================
#  Suchika Developer Aliases
#  Activate once per terminal session by dot-sourcing this file:
#
#     . .\scripts\dev-aliases.ps1
#
#  To activate automatically in every PowerShell session, add the line above
#  to your PowerShell profile:
#     notepad $PROFILE
#
#  Type  help-dev  after loading to see all available commands.
# ============================================================================

$_Root = Split-Path -Parent $PSScriptRoot   # repo root
$_Scripts = $PSScriptRoot                   # scripts/ folder

# ── Build commands ─────────────────────────────────────────────────────────────
# Builds compile + package (with Gradle cache). Use build-verify for pre-commit.

function build-profile   { & "$_Scripts\build-service.ps1" profile   @args }
function build-wealth    { & "$_Scripts\build-service.ps1" wealth    @args }
function build-health    { & "$_Scripts\build-service.ps1" health    @args }
function build-household { & "$_Scripts\build-service.ps1" household @args }
function build-gateway   { & "$_Scripts\build-service.ps1" gateway   @args }
function build-web       { & "$_Scripts\build-service.ps1" web       @args }
function build-shared    { & "$_Scripts\build-service.ps1" shared    @args }

function build-all {
    # Builds all services in correct dependency order (profile first, shared first).
    # Pass -NoCache to add --no-build-cache (slower but guaranteed clean bytecode).
    param([switch]$NoCache)
    $nc = if ($NoCache) { @('-NoCache') } else { @() }
    Write-Host "`n=== build-all ===" -ForegroundColor Cyan
    foreach ($svc in @('shared','profile','wealth','health','household','gateway','web')) {
        & "$_Scripts\build-service.ps1" $svc @nc
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [X]  build-all stopped at: $svc" -ForegroundColor Red
            return
        }
    }
    Write-Host "`n  [OK]  All services built." -ForegroundColor Green
}

# Full verification build: no cache + all tests + sonar. Run before committing.
function build-verify { & "$_Scripts\build-local.ps1" @args }

# ── Dev (hot-reload) commands ──────────────────────────────────────────────────
# Each opens a NEW terminal window running quarkusDev or npm start.
# Profile MUST be started before the other domain services.

function dev-profile   { & "$_Scripts\dev-service.ps1" profile   }
function dev-wealth    { & "$_Scripts\dev-service.ps1" wealth    }
function dev-health    { & "$_Scripts\dev-service.ps1" health    }
function dev-household { & "$_Scripts\dev-service.ps1" household }
function dev-gateway   { & "$_Scripts\dev-service.ps1" gateway   }
function dev-web       { & "$_Scripts\dev-service.ps1" web       }

function dev-all {
    # Starts all services in the correct order.
    # Waits for profile (8081) to be healthy before starting the rest.
    Write-Host "`n==> dev-all: starting Suchika (profile first)" -ForegroundColor Cyan

    & "$_Scripts\dev-service.ps1" profile

    Write-Host "     Waiting for profile on port 8081..." -ForegroundColor DarkGray
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        try {
            $r = Invoke-WebRequest -Uri 'http://localhost:8081/q/openapi' -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -lt 500) { break }
        } catch { }
        Write-Host '.' -NoNewline -ForegroundColor DarkGray
    }
    Write-Host ''

    foreach ($svc in @('wealth','health','household','gateway','web')) {
        & "$_Scripts\dev-service.ps1" $svc
    }

    Write-Host "`n  All services launched in separate windows." -ForegroundColor Green
    Write-Host "  Run: status   to see when everything is UP." -ForegroundColor DarkGray
}

# ── Test commands ──────────────────────────────────────────────────────────────

function test-profile   { & "$_Scripts\test-service.ps1" profile   }
function test-wealth    { & "$_Scripts\test-service.ps1" wealth    }
function test-health    { & "$_Scripts\test-service.ps1" health    }
function test-household { & "$_Scripts\test-service.ps1" household }
function test-gateway   { & "$_Scripts\test-service.ps1" gateway   }
function test-web       { & "$_Scripts\test-service.ps1" web       }
function test-shared    { & "$_Scripts\test-service.ps1" shared    }

function test-all {
    Push-Location $_Root
    try { & .\gradlew.bat test } finally { Pop-Location }
}

# ── Quality commands ───────────────────────────────────────────────────────────

function sonar-start { & "$_Scripts\sonar-start.ps1" }
function sonar-scan  { & "$_Scripts\sonar-scan.ps1"  }

# ── API client ─────────────────────────────────────────────────────────────────

function generate-api { & "$_Scripts\generate-api.ps1" }

# ── Database commands ──────────────────────────────────────────────────────────

function db-start { & "$_Scripts\db-start.ps1"              }
function db-reset { & "$_Scripts\db-reset.ps1" @args        }   # -Force to skip prompt
function db-shell { & "$_Scripts\db-shell.ps1" @args        }   # -AsAdmin for postgres user

# ── Stop / Status ──────────────────────────────────────────────────────────────

function stop-all { & "$_Scripts\stop-all.ps1" }
function status   { & "$_Scripts\health-check.ps1" }
function check    { & "$_Scripts\check-prerequisites.ps1" }

# ── Logs ──────────────────────────────────────────────────────────────────────

function logs {
    param([string]$Service = '', [int]$Lines = 50)
    & "$_Scripts\logs.ps1" $Service -Lines $Lines
}

# ── Maintenance ────────────────────────────────────────────────────────────────

function clean-builds { & "$_Scripts\clean-builds.ps1" }
function clean-all    { & "$_Scripts\clean-all.ps1" @args }   # -Force to skip prompt
function setup-dev    { & "$_Scripts\setup-dev.ps1" @args }

# ── Short aliases ──────────────────────────────────────────────────────────────

Set-Alias -Name bp   -Value build-profile    -Scope Global -Force
Set-Alias -Name bw   -Value build-wealth     -Scope Global -Force
Set-Alias -Name bh   -Value build-health     -Scope Global -Force
Set-Alias -Name bho  -Value build-household  -Scope Global -Force
Set-Alias -Name bg   -Value build-gateway    -Scope Global -Force
Set-Alias -Name bwb  -Value build-web        -Scope Global -Force
Set-Alias -Name ba   -Value build-all        -Scope Global -Force
Set-Alias -Name bv   -Value build-verify     -Scope Global -Force

Set-Alias -Name dp   -Value dev-profile      -Scope Global -Force
Set-Alias -Name dw   -Value dev-wealth       -Scope Global -Force
Set-Alias -Name dh   -Value dev-health       -Scope Global -Force
Set-Alias -Name dho  -Value dev-household    -Scope Global -Force
Set-Alias -Name dg   -Value dev-gateway      -Scope Global -Force
Set-Alias -Name dwb  -Value dev-web          -Scope Global -Force
Set-Alias -Name da   -Value dev-all          -Scope Global -Force

Set-Alias -Name tp   -Value test-profile     -Scope Global -Force
Set-Alias -Name tw   -Value test-wealth      -Scope Global -Force
Set-Alias -Name tsa  -Value test-all         -Scope Global -Force

Set-Alias -Name ss   -Value sonar-scan       -Scope Global -Force
Set-Alias -Name gapi -Value generate-api     -Scope Global -Force
Set-Alias -Name sa   -Value stop-all         -Scope Global -Force

# ── Help ───────────────────────────────────────────────────────────────────────

function help-dev {
    Write-Host @'

  ╔══════════════════════════════════════════════════════════════════════════╗
  ║                    Suchika Dev Aliases                                   ║
  ╚══════════════════════════════════════════════════════════════════════════╝

  BUILD  (Gradle cache ON by default; add -NoCache for clean bytecode)
  ─────────────────────────────────────────────────────────────────────────
  build-profile  [bp]       Build profile service (8081)
  build-wealth   [bw]       Build wealth service  (8082)
  build-health   [bh]       Build health service  (8083)
  build-household[bho]      Build household service (8084)
  build-gateway  [bg]       Build web-gateway BFF (8080)
  build-web      [bwb]      npm run build for React
  build-shared              Build shared (AppLogger, exceptions, ArchUnit)
  build-all      [ba]       Build all services in dependency order
  build-verify   [bv]       Full pre-commit check: no-cache + tests + sonar

  DEV MODE  (hot-reload — each opens a new terminal window)
  ─────────────────────────────────────────────────────────────────────────
  dev-profile    [dp]       quarkusDev profile  ← start THIS FIRST
  dev-wealth     [dw]       quarkusDev wealth
  dev-health     [dh]       quarkusDev health
  dev-household  [dho]      quarkusDev household
  dev-gateway    [dg]       quarkusDev gateway BFF
  dev-web        [dwb]      npm start React dev server (port 3000)
  dev-all        [da]       Start all 6 services (waits for profile first)

  TEST
  ─────────────────────────────────────────────────────────────────────────
  test-profile   [tp]       Tests for profile (domain + ports + adapters)
  test-wealth    [tw]       Tests for wealth
  test-health               Tests for health
  test-household            Tests for household
  test-gateway              Tests for gateway
  test-web                  npm run test:ci (Jest, single run)
  test-shared               ArchUnit + shared module tests
  test-all       [tsa]      All backend tests (./gradlew test)

  QUALITY
  ─────────────────────────────────────────────────────────────────────────
  sonar-start               Start SonarQube server + open browser
  sonar-scan     [ss]       Run sonar-scanner + open dashboard

  STATUS / CONTROL
  ─────────────────────────────────────────────────────────────────────────
  status                    HTTP/TCP health of all services
  stop-all       [sa]       Kill all services on ports 3000, 8080-8084
  logs [service]            Tail latest build/test log (omit for summary)

  DATABASE
  ─────────────────────────────────────────────────────────────────────────
  db-start                  Ensure PostgreSQL is running + open psql shell
  db-reset                  Drop + recreate app_db + run bootstrap.sql
  db-reset -Force           Skip confirmation prompt
  db-shell                  Open psql as app_user
  db-shell -AsAdmin         Open psql as postgres superuser

  API CLIENT
  ─────────────────────────────────────────────────────────────────────────
  generate-api   [gapi]     Regenerate web/src/api/generated.ts

  MAINTENANCE
  ─────────────────────────────────────────────────────────────────────────
  clean-builds              Gradle clean + remove web/build + .temp logs
  clean-all                 Full git clean (dry-run preview first)
  clean-all -Force          Skip confirmation prompt
  setup-dev                 First-time setup: prereqs + DB + .env + npm install
  check                     Verify all tools (Java, Node, psql) are installed

  ─────────────────────────────────────────────────────────────────────────
  Typical inner-loop workflow:
    1.  dp              ← start profile (ALWAYS first)
    2.  da              ← start all remaining services
    3.  status          ← confirm all UP
    4.  <edit code>
    5.  bp / bw / ...   ← rebuild changed service
    6.  tsa             ← run all tests
    7.  ss              ← sonar scan
    8.  bv              ← full pre-commit check

'@ -ForegroundColor Gray
}

# ── Activation message ─────────────────────────────────────────────────────────
Write-Host "`n  Suchika dev aliases loaded. Type " -NoNewline -ForegroundColor Green
Write-Host "help-dev" -NoNewline -ForegroundColor Cyan
Write-Host " to see all commands.`n" -ForegroundColor Green
