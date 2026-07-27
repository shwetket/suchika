#Requires -Version 5.1
# Shared dev-mode helper - called by dev-aliases.ps1, not directly by users.
# Opens a new terminal window running quarkusDev (or npm start for web).
# STARTUP ORDER: always start profile before other domain services.
#
# Ports, Gradle tasks and schemas come from scripts/services.json (single source
# of truth) via config.ps1 -- do not hardcode them here.
#
# Usage: .\scripts\dev-service.ps1 <service>
# <service>: profile | wealth | health | household | gateway | web
param(
    [Parameter(Mandatory)][ValidateSet('profile','wealth','health','household','gateway','web')]
    [string]$Service
)

$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot\config.ps1"
. "$PSScriptRoot\service-registry.ps1"

$svc  = Get-SuchikaService -Name $Service
$port = $svc.port

function IsPortBusy($p) {
    $null -ne (Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue)
}

if (IsPortBusy $port) {
    Write-Host "  [!]  Port $port already in use - $Service may already be running." -ForegroundColor Yellow
    Write-Host "       Run: stop-all   then retry, or run: status   to check." -ForegroundColor DarkGray
    return
}

if ($Service -eq 'web') {
    $cmd = "Set-Location '$root\web'; Write-Host 'React dev server  ->  http://localhost:$port' -ForegroundColor Cyan; npm start"
} else {
    $task = $svc.devTask
    $cmd  = "Set-Location '$root'; Write-Host '$Service dev server  ->  http://localhost:$port' -ForegroundColor Cyan; .\gradlew.bat $task"
}

# Prefer Windows Terminal tabs; fall back to a new PowerShell window
$wtAvailable = $null -ne (Get-Command wt.exe -ErrorAction SilentlyContinue)

if ($wtAvailable) {
    Start-Process wt.exe -ArgumentList "new-tab", "--title", $Service, "powershell.exe", "-NoExit", "-Command", $cmd
} else {
    Start-Process powershell.exe -ArgumentList "-NoExit", "-Command", $cmd
}

# The window we just opened owns the terminal PID, not the real java.exe/node.exe
# server process -- register the PID asynchronously once the port actually binds
# (see service-registry.ps1 for why). Returns immediately; does not block dev-all.
Register-SuchikaServiceAsync -Name $Service -Port $port

Write-Host "  [>>] $Service starting on port $port  (new window)" -ForegroundColor Green
Write-Host "       Run: status   to check when it is ready." -ForegroundColor DarkGray
