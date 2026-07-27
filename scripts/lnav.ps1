#Requires -Version 5.1
# Opens lnav watching all Suchika service log files in real-time.
# All 5 services write to ~/.suchika/logs/ when running in dev mode.
#
# Usage:
#   .\scripts\lnav.ps1              # watch all 5 service logs
#   .\scripts\lnav.ps1 profile      # watch only profile service
#   .\scripts\lnav.ps1 wealth,health # watch two services
#
# Prerequisites:
#   winget install tstack.lnav
#   Services must be running (dev-all or dp/dw/dh/dg)
param(
    [string]$Services = 'all'
)

$logDir = "$env:USERPROFILE\.suchika\logs"
$allServices = @('profile','wealth','health','household','gateway')

# Ensure log directory exists
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force $logDir | Out-Null
}

# Determine which log files to open
if ($Services -eq 'all') {
    $selected = $allServices
} else {
    $selected = $Services -split '[,\s]+' | Where-Object { $_ -ne '' }
    $invalid = $selected | Where-Object { $_ -notin $allServices }
    if ($invalid) {
        Write-Host "  Unknown service(s): $($invalid -join ', ')" -ForegroundColor Yellow
        Write-Host "  Valid: $($allServices -join ', ')" -ForegroundColor DarkGray
        return
    }
}

# Create empty log files for services not yet started (lnav needs them to exist)
foreach ($svc in $selected) {
    $f = "$logDir\$svc.log"
    if (-not (Test-Path $f)) {
        New-Item -ItemType File -Force $f | Out-Null
        Write-Host "  Created empty log: $f" -ForegroundColor DarkGray
        Write-Host "  Start $svc with: dev-$svc" -ForegroundColor DarkGray
    }
}

$logFiles = $selected | ForEach-Object { "$logDir\$_.log" }

Write-Host ""
Write-Host "  Opening lnav for: $($selected -join ', ')" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Key bindings:" -ForegroundColor DarkGray
Write-Host "    e / E       Jump to next/prev ERROR" -ForegroundColor DarkGray
Write-Host "    w / W       Jump to next/prev WARN" -ForegroundColor DarkGray
Write-Host "    /           Search (regex)" -ForegroundColor DarkGray
Write-Host "    n / N       Next/prev search match" -ForegroundColor DarkGray
Write-Host "    :filter-in  Show only lines matching a pattern" -ForegroundColor DarkGray
Write-Host "    :filter-out Hide lines matching a pattern" -ForegroundColor DarkGray
Write-Host "    ;           SQL query mode (e.g. SELECT service, body FROM suchika_log)" -ForegroundColor DarkGray
Write-Host "    TAB         Switch between log files" -ForegroundColor DarkGray
Write-Host "    q           Quit" -ForegroundColor DarkGray
Write-Host ""

lnav $logFiles
