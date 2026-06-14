# Health report for all Suchika services.
# Usage: .\scripts\health-check.ps1

$UP = 0; $DOWN = 0

function CheckHttp($name, $url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        Write-Host ("  [UP]   {0,-22}  {1}  (HTTP {2})" -f $name, $url, $r.StatusCode) -ForegroundColor Green
        $script:UP++
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code) {
            # Got a response (4xx/5xx) — service is up, just unhappy
            Write-Host ("  [UP]   {0,-22}  {1}  (HTTP {2})" -f $name, $url, $code) -ForegroundColor Green
            $script:UP++
        } else {
            Write-Host ("  [DOWN] {0,-22}  {1}" -f $name, $url) -ForegroundColor Red
            $script:DOWN++
        }
    }
}

function CheckTcp($name, $host, $port) {
    try {
        $tc = New-Object System.Net.Sockets.TcpClient
        $tc.Connect($host, $port)
        $tc.Close()
        Write-Host ("  [UP]   {0,-22}  {1}:{2}" -f $name, $host, $port) -ForegroundColor Green
        $script:UP++
    } catch {
        Write-Host ("  [DOWN] {0,-22}  {1}:{2}" -f $name, $host, $port) -ForegroundColor Red
        $script:DOWN++
    }
}

Write-Host "`n=== Suchika Health Report === $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')`n" -ForegroundColor Cyan

Write-Host "Database" -ForegroundColor White
CheckTcp  "PostgreSQL"        "localhost" 5432

Write-Host "`nBackend Services" -ForegroundColor White
CheckHttp "Profile"           "http://localhost:8081/q/openapi"
CheckHttp "Wealth"            "http://localhost:8082/q/openapi"
CheckHttp "Health"            "http://localhost:8083/q/openapi"
CheckHttp "Household"         "http://localhost:8084/q/openapi"
CheckHttp "Web Gateway (BFF)" "http://localhost:8080/q/openapi"

Write-Host "`nFrontend" -ForegroundColor White
CheckHttp "React Dev Server"  "http://localhost:3000"

$total = $UP + $DOWN
Write-Host ""
if ($DOWN -eq 0) {
    Write-Host "All $total services UP." -ForegroundColor Green
} else {
    Write-Host "$UP/$total services UP -- $DOWN DOWN." -ForegroundColor Yellow
}
Write-Host ""
