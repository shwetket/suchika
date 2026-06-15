#Requires -Version 5.1
# Ensures PostgreSQL is running and opens a psql shell to app_db.
# Usage: .\scripts\db-start.ps1

$psqlExe = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

function FindPsql {
    $fromPath = (Get-Command psql -ErrorAction SilentlyContinue)?.Source
    if ($fromPath) { return $fromPath }
    if (Test-Path $psqlExe) { return $psqlExe }
    # Search common PostgreSQL install locations
    $found = Get-ChildItem 'C:\Program Files\PostgreSQL' -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
             Sort-Object FullName -Descending | Select-Object -First 1
    return $found?.FullName
}

# ── Find and start PostgreSQL service ─────────────────────────────────────────
Write-Host "`n==> PostgreSQL" -ForegroundColor Cyan

$pgSvc = Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Select-Object -First 1

if (-not $pgSvc) {
    Write-Host "  [!]  No PostgreSQL service found. Is PostgreSQL installed?" -ForegroundColor Yellow
    Write-Host "       Download: https://www.postgresql.org/download/windows/" -ForegroundColor DarkGray
    exit 1
}

if ($pgSvc.Status -eq 'Running') {
    Write-Host "  [OK]  $($pgSvc.Name) already running" -ForegroundColor Green
} else {
    Write-Host "  [>>] Starting $($pgSvc.Name)..." -ForegroundColor Yellow
    try {
        Start-Service $pgSvc.Name
        Write-Host "  [OK]  $($pgSvc.Name) started" -ForegroundColor Green
    } catch {
        Write-Host "  [X]  Failed to start $($pgSvc.Name): $_" -ForegroundColor Red
        Write-Host "       Try running as Administrator." -ForegroundColor Yellow
        exit 1
    }
}

# ── Verify port 5432 is listening ──────────────────────────────────────────────
Start-Sleep -Milliseconds 500
try {
    $tc = New-Object System.Net.Sockets.TcpClient
    $tc.Connect('localhost', 5432)
    $tc.Close()
    Write-Host "  [OK]  Listening on localhost:5432" -ForegroundColor Green
} catch {
    Write-Host "  [!]  Port 5432 not yet ready — wait a moment and retry." -ForegroundColor Yellow
}

# ── Open db-shell ──────────────────────────────────────────────────────────────
$psql = FindPsql
if ($psql) {
    Write-Host "`n  Opening psql shell as postgres  (type \q to exit)" -ForegroundColor DarkGray
    & $psql -U postgres -d app_db
} else {
    Write-Host "  [!]  psql not found. Connect manually:" -ForegroundColor Yellow
    Write-Host "       psql -U postgres -d app_db" -ForegroundColor DarkGray
}
