#Requires -Version 5.1
# Shared dev-mode helper - called by dev-aliases.ps1, not directly by users.
# Opens a new terminal window running quarkusDev (or npm start for web).
# STARTUP ORDER: always start profile before other domain services.
#
# Usage: .\scripts\dev-service.ps1 <service>
# <service>: profile | wealth | health | household | gateway | web
param(
    [Parameter(Mandatory)][ValidateSet('profile','wealth','health','household','gateway','web')]
    [string]$Service
)

$root = Split-Path -Parent $PSScriptRoot

$gradleTask = @{
    profile   = ':application:domain:profile:adapters:quarkusDev'
    wealth    = ':application:domain:wealth:adapters:quarkusDev'
    health    = ':application:domain:health:adapters:quarkusDev'
    household = ':application:domain:household:adapters:quarkusDev'
    gateway   = ':application:web-gateway:quarkusDev'
}

$port = @{
    profile   = 8081
    wealth    = 8082
    health    = 8083
    household = 8084
    gateway   = 8080
    web       = 3000
}

function IsPortBusy($p) {
    $null -ne (Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue)
}

if (IsPortBusy $port[$Service]) {
    Write-Host "  [!]  Port $($port[$Service]) already in use - $Service may already be running." -ForegroundColor Yellow
    Write-Host "       Run: stop-all   then retry, or run: status   to check." -ForegroundColor DarkGray
    return
}

if ($Service -eq 'web') {
    $cmd = "Set-Location '$root\web'; Write-Host 'React dev server  ->  http://localhost:3000' -ForegroundColor Cyan; npm start"
} else {
    $task = $gradleTask[$Service]
    $cmd  = "Set-Location '$root'; Write-Host '$Service dev server  ->  http://localhost:$($port[$Service])' -ForegroundColor Cyan; .\gradlew.bat $task"
}

# Prefer Windows Terminal tabs; fall back to a new PowerShell window
$wtAvailable = $null -ne (Get-Command wt.exe -ErrorAction SilentlyContinue)

if ($wtAvailable) {
    Start-Process wt.exe -ArgumentList "new-tab", "--title", $Service, "powershell.exe", "-NoExit", "-Command", $cmd
} else {
    Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $cmd
}

Write-Host "  [>>] $Service starting on port $($port[$Service])  (new window)" -ForegroundColor Green
Write-Host "       Run: status   to check when it is ready." -ForegroundColor DarkGray
