#Requires -Version 5.1
# Kills all Suchika dev processes. Checks the PID-file registry first (see
# service-registry.ps1) -- if a service has a valid, live PID record, that exact
# process is killed. Falls back to today's port-based "whatever is LISTENing on
# this port" detection only when no PID file exists, so anything started the old
# way (or by a script that predates the registry) still gets cleaned up.
# Ports/service names come from scripts/services.json (single source of truth).
# Usage: .\scripts\stop-all.ps1
#        .\scripts\stop-all.ps1 -Service wealth   # stop ONLY wealth
#
# -Service scopes the exact same registry-first/port-fallback kill loop to one
# named entry from scripts/services.json instead of all of them. Omitting it
# keeps the existing "stop everything" behavior as the default -- unchanged.
param(
    [string]$Service = ''
)

. "$PSScriptRoot\config.ps1"
. "$PSScriptRoot\service-registry.ps1"

$killed  = 0
$skipped = 0

Write-Host "`n==> Stopping Suchika services" -ForegroundColor Cyan

$targetNames = if ($Service) { , (Get-SuchikaService -Name $Service).name } else { Get-SuchikaServiceNames }

foreach ($name in $targetNames) {
    $svc  = Get-SuchikaService -Name $name
    $port = $svc.port

    $registered = Get-SuchikaRunningPid -Name $name
    if ($registered) {
        Stop-Process -Id $registered.pid -Force -ErrorAction SilentlyContinue
        Remove-SuchikaServicePid -Name $name
        Write-Host ("  [OK]  {0,-12}  port {1}  killed PID {2} ({3})  [registry]" -f $name, $port, $registered.pid, $registered.processName) -ForegroundColor Green
        $killed++
        continue
    }

    # Fallback: port-based detection (pre-registry behavior)
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host ("  [--]  {0,-12}  port {1}  (not running)" -f $name, $port) -ForegroundColor DarkGray
        $skipped++
        continue
    }
    foreach ($conn in $conns) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Write-Host ("  [OK]  {0,-12}  port {1}  killed PID {2} ({3})  [port fallback]" -f $name, $port, $proc.Id, $proc.ProcessName) -ForegroundColor Green
            $killed++
        }
    }
}

Write-Host ""
if ($killed -eq 0) {
    Write-Host "  No services were running." -ForegroundColor DarkGray
} else {
    Write-Host "  Stopped $killed service(s).  $skipped not running." -ForegroundColor Green
}
Write-Host ""
