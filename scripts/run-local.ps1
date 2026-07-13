#Requires -Version 5.1
# Headless "just run the app" mode -- starts all 5 backend services + the frontend
# with NO GUI terminal windows on any OS. bash's dev-aliases.sh already backgrounds
# processes this way (there's no "new window" concept in a Codespaces/Linux
# terminal); this brings Windows to parity. dev-service.ps1 / dev-all (da) still open
# a visible window per service on Windows -- keep using those for active development,
# where watching Quarkus's live hot-reload console output matters. This script is for
# "I just want the app running," not for developing against it.
#
# Reuses scripts/services.json (ports/tasks/startOrder) via config.ps1, and the PID
# registry (scripts/service-registry.ps1) from Phase 1 -- does not duplicate either.
# Logs go to the same ~/.suchika/logs/<service>.log path bash already uses.
#
# Usage: .\scripts\run-local.ps1  [-TimeoutSec 180]
#        .\scripts\run-local.ps1  -Service wealth   # start ONLY wealth, headlessly
#
# -Service targets exactly one entry from scripts/services.json (any of profile,
# wealth, health, household, gateway, web) -- same headless start + PID-registry
# tracking + health-wait as the full-stack path below, just scoped to one service.
# Omitting -Service keeps the existing "start everything in dependency order"
# behavior as the default -- unchanged.
param(
    [int]$TimeoutSec = 180,
    [string]$Service = ''
)

$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\config.ps1"
. "$PSScriptRoot\service-registry.ps1"

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "  [!]   $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host "  [X]   $msg" -ForegroundColor Red }

function IsPortBusy($p) {
    $null -ne (Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue)
}

function Start-SuchikaHeadless {
    param([Parameter(Mandatory)]$Svc)
    $name = $Svc.name
    $port = $Svc.port
    $log  = Join-Path $SuchikaLogDir "$name.log"

    if (IsPortBusy $port) {
        Warn "$name -- port $port already in use, skipping (already running?)"
        return
    }

    # Write a tiny wrapper .cmd instead of fighting Start-Process's ArgumentList
    # quoting for a nested "cd && task >> log 2>&1" one-liner. cmd.exe combines
    # stdout+stderr into ONE appended file, matching bash's `>> log 2>&1` exactly --
    # the same file lnav-dev already watches on both platforms.
    $wrapperDir = Join-Path $env:TEMP 'suchika-run-local'
    $null = New-Item -ItemType Directory -Force -Path $wrapperDir -ErrorAction SilentlyContinue
    $wrapper = Join-Path $wrapperDir "$name.cmd"

    if ($name -eq 'web') {
        # npm has no independent file-logger of its own -- $log is the only writer,
        # so redirecting its stdout/stderr straight into it is safe.
        $lines = @(
            '@echo off',
            "cd /d `"$root\web`"",
            "call npm start >> `"$log`" 2>&1"
        )
    } else {
        # Use gradlew.bat's absolute path, not a bare "gradlew.bat" relying on cwd
        # search after "cd /d" -- confirmed during testing that some Windows setups
        # (sandboxed/virtualized shells in particular) resolve a cwd-relative bare
        # command name unreliably for process creation even though `cd`/`dir` in the
        # same batch see the file fine. An absolute path removes the ambiguity
        # entirely and costs nothing on a normal machine.
        $gradlewPath = Join-Path $root 'gradlew.bat'
        # IMPORTANT: redirect to a separate *.console.log, never to $log. Each
        # backend's own application.properties sets %dev.quarkus.log.file.path to
        # this exact $log path, so Quarkus opens/rotates that file itself on a
        # second, independent handle. On Windows that collides with cmd.exe's `>>`
        # handle (confirmed during testing: intermittent
        # "LogManager error ... The process cannot access the file because it is
        # being used by another process", i.e. SizeRotatingFileHandler failing to
        # rotate/open a file cmd.exe still has open) -- it can recover via Quarkus's
        # own retry, but isn't reliable. $log stays Quarkus's structured log,
        # written solely by Quarkus; the console sidecar only exists to capture
        # Gradle's own pre-Quarkus output (daemon/compile) for diagnosing a service
        # that fails before Quarkus's logger ever attaches.
        $consoleLog = Join-Path $SuchikaLogDir "$name.console.log"
        $lines = @(
            '@echo off',
            "cd /d `"$root`"",
            "call `"$gradlewPath`" $($Svc.devTask) >> `"$consoleLog`" 2>&1"
        )
    }
    Set-Content -Path $wrapper -Value $lines -Encoding ASCII

    # -WindowStyle Hidden: no window on any Windows session (console or RDP).
    # -PassThru's .Id is the cmd.exe wrapper's PID, not the real java/node server
    # process it spawns -- same situation as dev-service.ps1's GUI-window PID, and
    # solved the same way: Register-SuchikaServiceAsync (reused from Phase 1, not
    # duplicated here) polls the port and persists the REAL owning process once it
    # binds, which is what stop-local/stop-all actually need to kill cleanly.
    Start-Process -FilePath $wrapper -WindowStyle Hidden -PassThru | Out-Null
    Register-SuchikaServiceAsync -Name $name -Port $port -TimeoutSec $TimeoutSec

    Write-Host "  [>>] $name starting headlessly on port $port  (log: $log)" -ForegroundColor Green
}

if ($Service) {
    # Single-service path: same headless-start + PID-registry + health-wait logic
    # as the full-stack path below, just scoped to one scripts/services.json entry.
    # Get-SuchikaService throws on an unknown name, so a typo fails fast here
    # instead of silently doing nothing.
    $svc = Get-SuchikaService -Name $Service
    Step "run-local -Service $Service`: starting only $Service headlessly"
    Start-SuchikaHeadless -Svc $svc
    Write-Host "     Waiting for $Service ($(Get-SuchikaHealthUrl -Name $Service))..." -ForegroundColor DarkGray
    if (Wait-SuchikaHealthy -Name $Service -TimeoutSec $TimeoutSec) {
        OK "$Service healthy"
    } else {
        Fail "$Service did not become healthy within ${TimeoutSec}s -- check log: $(Join-Path $SuchikaLogDir "$Service.log")"
        exit 1
    }
    Write-Host ""
    Write-Host "  $Service running headlessly, no GUI window opened. Other services untouched." -ForegroundColor Green
    Write-Host ""
    exit 0
}

Step "run-local: starting Suchika headlessly (no GUI windows)"

# -- Profile first -- everything else FKs into profile.profile -------------------
Start-SuchikaHeadless -Svc (Get-SuchikaService -Name 'profile')
Write-Host "     Waiting for profile ($(Get-SuchikaHealthUrl -Name 'profile'))..." -ForegroundColor DarkGray
if (-not (Wait-SuchikaHealthy -Name 'profile' -TimeoutSec $TimeoutSec)) {
    Fail "profile did not become healthy within ${TimeoutSec}s -- check log: $(Join-Path $SuchikaLogDir 'profile.log')"
    Write-Host "  Aborting -- other services depend on profile." -ForegroundColor Red
    exit 1
}
OK "profile healthy"

# -- wealth / health / household in parallel --------------------------------------
foreach ($name in @('wealth', 'health', 'household')) {
    Start-SuchikaHeadless -Svc (Get-SuchikaService -Name $name)
}

$allReady = $true
foreach ($name in @('wealth', 'health', 'household')) {
    if (Wait-SuchikaHealthy -Name $name -TimeoutSec $TimeoutSec) {
        OK "$name healthy"
    } else {
        Warn "$name did not become healthy within ${TimeoutSec}s -- check log: $(Join-Path $SuchikaLogDir "$name.log")"
        $allReady = $false
    }
}

# -- Gateway (BFF) -- needs wealth/health/household reachable, not strictly blocking
Start-SuchikaHeadless -Svc (Get-SuchikaService -Name 'gateway')
if (Wait-SuchikaHealthy -Name 'gateway' -TimeoutSec $TimeoutSec) {
    OK "gateway healthy"
} else {
    Warn "gateway did not become healthy within ${TimeoutSec}s -- check log: $(Join-Path $SuchikaLogDir 'gateway.log')"
    $allReady = $false
}

# -- Frontend -----------------------------------------------------------------------
Start-SuchikaHeadless -Svc (Get-SuchikaService -Name 'web')
if (Wait-SuchikaHealthy -Name 'web' -TimeoutSec $TimeoutSec) {
    OK "frontend reachable"
} else {
    Warn "frontend did not respond within ${TimeoutSec}s -- check log: $(Join-Path $SuchikaLogDir 'web.log')"
    $allReady = $false
}

Write-Host ""
if ($allReady) {
    Write-Host "  All services running headlessly, no GUI windows opened." -ForegroundColor Green
    Write-Host "  Run: status        to confirm  |  stop-local   to stop everything." -ForegroundColor DarkGray
} else {
    Write-Host "  Some services may not be fully ready -- run: status   for details, or check the logs above." -ForegroundColor Yellow
}
Write-Host ""
