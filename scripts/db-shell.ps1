#Requires -Version 5.1
# Opens a psql shell connected to app_db.
# Usage: .\scripts\db-shell.ps1 [-AsAdmin]
#   -AsAdmin  : connect as postgres superuser (for schema changes, bootstrap)
#   (default) : connect as app_user (same user the services use)
param([switch]$AsAdmin)

$psqlExe = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

function FindPsql {
    $fromPath = (Get-Command psql -ErrorAction SilentlyContinue)?.Source
    if ($fromPath) { return $fromPath }
    if (Test-Path $psqlExe) { return $psqlExe }
    $found = Get-ChildItem 'C:\Program Files\PostgreSQL' -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
             Sort-Object FullName -Descending | Select-Object -First 1
    return $found?.FullName
}

$psql = FindPsql
if (-not $psql) {
    Write-Host "  [X]  psql not found. Add PostgreSQL\bin to PATH." -ForegroundColor Red; exit 1
}

if ($AsAdmin) {
    $user = 'postgres'
    Write-Host "`n  Connecting as postgres (superuser)  →  \q to exit" -ForegroundColor Yellow
} else {
    $user = 'app_user'
    Write-Host "`n  Connecting as app_user  →  \q to exit" -ForegroundColor Cyan
}

Write-Host "  $psql -U $user -d app_db`n" -ForegroundColor DarkGray
& $psql -U $user -d app_db
