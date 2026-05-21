#Requires -Version 5.1
# Tails the latest build or test log for a service.
# Usage:
#   .\scripts\logs.ps1                 # list newest log from each service
#   .\scripts\logs.ps1 profile         # tail latest log for profile
#   .\scripts\logs.ps1 profile -Lines 100
param(
    [string]$Service = '',
    [int]$Lines = 50
)

$root    = Split-Path -Parent $PSScriptRoot
$logBase = Join-Path $root '.temp\logs'

if (-not (Test-Path $logBase)) {
    Write-Host "`n  No logs yet. Run a build or test first." -ForegroundColor Yellow
    Write-Host "  Example: build-profile" -ForegroundColor DarkGray
    return
}

if (-not $Service) {
    # ── Summary: newest log per service ───────────────────────────────────────
    Write-Host "`n=== Build / Test Logs ===" -ForegroundColor Cyan
    $services = Get-ChildItem $logBase -Directory | Sort-Object Name
    if (-not $services) {
        Write-Host "  (empty — run a build first)" -ForegroundColor DarkGray
        return
    }
    foreach ($svc in $services) {
        $latest = Get-ChildItem $svc.FullName -Filter '*.log' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latest) {
            $age = (Get-Date) - $latest.LastWriteTime
            $ageStr = if ($age.TotalMinutes -lt 60) { "$([int]$age.TotalMinutes)m ago" } else { "$([int]$age.TotalHours)h ago" }
            Write-Host ("  {0,-12}  {1,-30}  {2}" -f $svc.Name, $latest.Name, $ageStr) -ForegroundColor Gray
        } else {
            Write-Host ("  {0,-12}  (no logs)" -f $svc.Name) -ForegroundColor DarkGray
        }
    }
    Write-Host "`n  Run: logs <service>   to tail a specific log." -ForegroundColor DarkGray
    return
}

# ── Tail specific service ──────────────────────────────────────────────────────
$svcDir = Join-Path $logBase $Service
if (-not (Test-Path $svcDir)) {
    Write-Host "`n  No logs for '$Service'. Valid services: profile, wealth, health, household, gateway, web, shared" -ForegroundColor Yellow
    return
}

$latest = Get-ChildItem $svcDir -Filter '*.log' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $latest) {
    Write-Host "`n  No log files in $svcDir" -ForegroundColor Yellow
    return
}

Write-Host "`n==> $($latest.FullName)" -ForegroundColor Cyan
Write-Host "    (last $Lines lines — Ctrl+C to stop live tail)`n" -ForegroundColor DarkGray
Get-Content $latest.FullName -Tail $Lines -Wait
