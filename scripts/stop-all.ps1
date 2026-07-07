#Requires -Version 5.1
# Kills all Suchika dev processes by port.
# Ports: 3000 (React), 8080 (gateway), 8081-8084 (domain services).
# Usage: .\scripts\stop-all.ps1

$ports   = @(3000, 8080, 8081, 8082, 8083, 8084)
$names   = @{3000='web'; 8080='gateway'; 8081='profile'; 8082='wealth'; 8083='health'; 8084='household'}
$killed  = 0
$skipped = 0

Write-Host "`n==> Stopping Suchika services" -ForegroundColor Cyan

foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) {
        Write-Host ("  [--]  {0,-12}  port {1}  (not running)" -f $names[$port], $port) -ForegroundColor DarkGray
        $skipped++
        continue
    }
    foreach ($conn in $conns) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Write-Host ("  [OK]  {0,-12}  port {1}  killed PID {2} ({3})" -f $names[$port], $port, $proc.Id, $proc.ProcessName) -ForegroundColor Green
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
