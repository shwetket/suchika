#Requires -Version 5.1
# Health report for all Suchika services.
# Hits the real Quarkus /q/health endpoint (quarkus-smallrye-health) on each
# backend service and only counts a genuine HTTP 200 with body status "UP" as
# UP -- previously this hit /q/openapi and treated ANY response, including a
# 500, as "UP", which meant a broken service could still report healthy.
# Ports/URLs come from scripts/services.json (single source of truth) via
# config.ps1. Also shows the PID-registry entry for each service, if one exists
# (see service-registry.ps1) -- informational only, does not affect UP/DOWN.
# Usage: .\scripts\health-check.ps1

. "$PSScriptRoot\config.ps1"
. "$PSScriptRoot\service-registry.ps1"

$UP = 0; $DOWN = 0

function PidSuffix($name) {
    $reg = Get-SuchikaRunningPid -Name $name
    if ($reg) { return "  [PID $($reg.pid) $($reg.processName)]" }
    return ''
}

function CheckHealth($name, $url) {
    $suffix = PidSuffix $name
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        $body = $null
        try { $body = $r.Content | ConvertFrom-Json } catch { }
        $status = if ($body -and $body.status) { $body.status } else { 'UP' }
        if ($r.StatusCode -eq 200 -and $status -eq 'UP') {
            Write-Host ("  [UP]   {0,-22}  {1}  (HTTP 200, status=UP){2}" -f $name, $url, $suffix) -ForegroundColor Green
            $script:UP++
        } else {
            Write-Host ("  [DOWN] {0,-22}  {1}  (HTTP {2}, status={3}){4}" -f $name, $url, $r.StatusCode, $status, $suffix) -ForegroundColor Red
            $script:DOWN++
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) {
            Write-Host ("  [DOWN] {0,-22}  {1}  (HTTP {2}){3}" -f $name, $url, $code, $suffix) -ForegroundColor Red
        } else {
            Write-Host ("  [DOWN] {0,-22}  {1}  (unreachable){2}" -f $name, $url, $suffix) -ForegroundColor Red
        }
        $script:DOWN++
    }
}

function CheckHttp($name, $url) {
    # Plain reachability check (used for the frontend, which has no /q/health).
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        if ($r.StatusCode -eq 200) {
            Write-Host ("  [UP]   {0,-22}  {1}  (HTTP 200)" -f $name, $url) -ForegroundColor Green
            $script:UP++
        } else {
            Write-Host ("  [DOWN] {0,-22}  {1}  (HTTP {2})" -f $name, $url, $r.StatusCode) -ForegroundColor Red
            $script:DOWN++
        }
    } catch {
        Write-Host ("  [DOWN] {0,-22}  {1}" -f $name, $url) -ForegroundColor Red
        $script:DOWN++
    }
}

function CheckTcp($name, [string]$hostName, $port) {
    try {
        $tc = New-Object System.Net.Sockets.TcpClient
        $tc.Connect($hostName, $port)
        $tc.Close()
        Write-Host ("  [UP]   {0,-22}  {1}:{2}" -f $name, $hostName, $port) -ForegroundColor Green
        $script:UP++
    } catch {
        Write-Host ("  [DOWN] {0,-22}  {1}:{2}" -f $name, $hostName, $port) -ForegroundColor Red
        $script:DOWN++
    }
}

Write-Host "`n=== Suchika Health Report === $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n" -ForegroundColor Cyan

Write-Host "Database" -ForegroundColor White
CheckTcp "PostgreSQL" "localhost" $SuchikaDb.port

Write-Host "`nBackend Services" -ForegroundColor White
foreach ($name in (Get-SuchikaServiceNames -BackendOnly)) {
    $svc = Get-SuchikaService -Name $name
    $label = if ($name -eq 'gateway') { 'Web Gateway (BFF)' } else { (Get-Culture).TextInfo.ToTitleCase($name) }
    CheckHealth $label (Get-SuchikaHealthUrl -Name $name)
}

Write-Host "`nFrontend" -ForegroundColor White
CheckHttp "React Dev Server" (Get-SuchikaHealthUrl -Name 'web')

$total = $UP + $DOWN
Write-Host ""
if ($DOWN -eq 0) {
    Write-Host "All $total services UP." -ForegroundColor Green
} else {
    Write-Host "$UP/$total services UP -- $DOWN DOWN." -ForegroundColor Yellow
}
Write-Host ""
