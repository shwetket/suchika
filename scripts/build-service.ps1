#Requires -Version 5.1
# Shared build helper — called by dev-aliases.ps1, not directly by users.
# Usage: .\scripts\build-service.ps1 <service> [-NoCache]
#
# <service>: profile | wealth | health | household | gateway | shared | web
# -NoCache : adds --no-build-cache (use for pre-commit verification only)
param(
    [Parameter(Mandatory)][ValidateSet('profile','wealth','health','household','gateway','shared','web')]
    [string]$Service,
    [switch]$NoCache
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "  [X]   $msg" -ForegroundColor Red }

# ── Log directory ──────────────────────────────────────────────────────────────
$logDir  = Join-Path $root ".temp\logs\$Service"
$null    = New-Item -ItemType Directory -Force -Path $logDir
$stamp   = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
$logFile = Join-Path $logDir "build_$stamp.log"

# ── Gradle task mapping ────────────────────────────────────────────────────────
$gradleTasks = @{
    profile   = @(':application:domain:profile:domain:build',
                   ':application:domain:profile:ports:build',
                   ':application:domain:profile:adapters:build')
    wealth    = @(':application:domain:wealth:domain:build',
                   ':application:domain:wealth:ports:build',
                   ':application:domain:wealth:adapters:build')
    health    = @(':application:domain:health:domain:build',
                   ':application:domain:health:ports:build',
                   ':application:domain:health:adapters:build')
    household = @(':application:domain:household:domain:build',
                   ':application:domain:household:ports:build',
                   ':application:domain:household:adapters:build')
    gateway   = @(':application:web-gateway:build')
    shared    = @(':shared:build')
}

Step "Building $Service  →  $logFile"
$start = Get-Date

if ($Service -eq 'web') {
    # ── Frontend build ─────────────────────────────────────────────────────────
    Push-Location (Join-Path $root 'web')
    try {
        & npm run build 2>&1 | Tee-Object -FilePath $logFile
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} else {
    # ── Gradle build ───────────────────────────────────────────────────────────
    $tasks = $gradleTasks[$Service]
    $extra = if ($NoCache) { @('--no-build-cache') } else { @() }
    Push-Location $root
    try {
        & .\gradlew.bat @tasks @extra 2>&1 | Tee-Object -FilePath $logFile
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

$elapsed = [int]([datetime]::Now - $start).TotalSeconds
$mins    = [int]($elapsed / 60)
$secs    = $elapsed % 60

if ($exit -eq 0) {
    OK "$Service built in ${mins}m${secs}s"
    Write-Host "     Log: $logFile" -ForegroundColor DarkGray
} else {
    Fail "$Service build FAILED in ${mins}m${secs}s"
    Write-Host "     Log: $logFile" -ForegroundColor DarkGray
    Write-Host "     Run: logs $Service   to tail the output" -ForegroundColor Yellow
    exit $exit
}
