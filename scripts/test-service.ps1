#Requires -Version 5.1
# Shared test helper — called by dev-aliases.ps1, not directly by users.
# Runs tests for a single domain and logs output to .temp\logs\{service}\.
#
# Usage: .\scripts\test-service.ps1 <service>
# <service>: profile | wealth | health | household | gateway | shared | web
param(
    [Parameter(Mandatory)][ValidateSet('profile','wealth','health','household','gateway','shared','web')]
    [string]$Service
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function OK($msg)   { Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "  [X]   $msg" -ForegroundColor Red }

$logDir  = Join-Path $root ".temp\logs\$Service"
$null    = New-Item -ItemType Directory -Force -Path $logDir
$stamp   = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
$logFile = Join-Path $logDir "test_$stamp.log"

$gradleTasks = @{
    profile   = @(':application:domain:profile:domain:test',
                   ':application:domain:profile:ports:test',
                   ':application:domain:profile:adapters:test')
    wealth    = @(':application:domain:wealth:domain:test',
                   ':application:domain:wealth:ports:test',
                   ':application:domain:wealth:adapters:test')
    health    = @(':application:domain:health:domain:test',
                   ':application:domain:health:ports:test',
                   ':application:domain:health:adapters:test')
    household = @(':application:domain:household:domain:test',
                   ':application:domain:household:ports:test',
                   ':application:domain:household:adapters:test')
    gateway   = @(':application:web-gateway:test')
    shared    = @(':shared:test')
}

Step "Testing $Service  →  $logFile"
$start = Get-Date

if ($Service -eq 'web') {
    Push-Location (Join-Path $root 'web')
    try {
        & npm run test:ci 2>&1 | Tee-Object -FilePath $logFile
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} else {
    $tasks = $gradleTasks[$Service]
    Push-Location $root
    try {
        & .\gradlew.bat @tasks 2>&1 | Tee-Object -FilePath $logFile
        $exit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

$elapsed = [int]([datetime]::Now - $start).TotalSeconds

if ($exit -eq 0) {
    OK "$Service tests passed  ($($elapsed)s)  →  $logFile"
} else {
    Fail "$Service tests FAILED  ($($elapsed)s)"
    Write-Host "     Log: $logFile" -ForegroundColor DarkGray
    exit $exit
}
