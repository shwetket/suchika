#Requires -Version 5.1
# Drops and recreates app_db, then re-runs 00_bootstrap.sql.
# WARNING: destroys all local data. Intended for use before v1.0 (all data is ephemeral).
# Usage: .\scripts\db-reset.ps1  [-Force]
param([switch]$Force)

$root    = Split-Path -Parent $PSScriptRoot
$boot    = Join-Path $root 'application\flyway\00_bootstrap.sql'
$psqlExe = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

function FindPsql {
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    if (Test-Path $psqlExe) { return $psqlExe }
    $found = Get-ChildItem 'C:\Program Files\PostgreSQL' -Recurse -Filter 'psql.exe' -ErrorAction SilentlyContinue |
             Sort-Object FullName -Descending | Select-Object -First 1
    if ($found) { return $found.FullName }
    return $null
}

if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { 'local_dev_only' }
}

$psql = FindPsql
if (-not $psql) {
    Write-Host "  [X]  psql not found. Add PostgreSQL\bin to PATH." -ForegroundColor Red; exit 1
}
if (-not (Test-Path $boot)) {
    Write-Host "  [X]  Bootstrap not found: $boot" -ForegroundColor Red; exit 1
}

Write-Host "`n  WARNING: This will DROP app_db and lose all local data." -ForegroundColor Red
Write-Host "  (Pre-v1.0 all data is ephemeral - this is expected.)`n" -ForegroundColor DarkGray

if (-not $Force) {
    $confirm = Read-Host "  Type 'reset' to confirm"
    if ($confirm -ne 'reset') {
        Write-Host "  Aborted." -ForegroundColor Yellow; return
    }
}

function RunPsql([string]$db, [string]$sql) {
    $out = & $psql -U postgres -d $db -c $sql 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [X]  psql error: $out" -ForegroundColor Red; exit 1
    }
    return $out
}

Write-Host "`n==> Resetting database" -ForegroundColor Cyan

# Terminate existing connections before dropping
RunPsql 'postgres' "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='app_db' AND pid <> pg_backend_pid();" | Out-Null
Write-Host "  [OK]  Connections terminated" -ForegroundColor Green

RunPsql 'postgres' 'DROP DATABASE IF EXISTS app_db;' | Out-Null
Write-Host "  [OK]  app_db dropped" -ForegroundColor Green

RunPsql 'postgres' 'CREATE DATABASE app_db;' | Out-Null
Write-Host "  [OK]  app_db created" -ForegroundColor Green

$out = & $psql -U postgres -d app_db -f $boot 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [X]  Bootstrap failed:`n$out" -ForegroundColor Red; exit 1
}
Write-Host "  [OK]  Bootstrap SQL applied (schemas + app_user created)" -ForegroundColor Green

# Show schema summary
Write-Host "`n  Schemas in app_db:" -ForegroundColor Cyan
& $psql -U postgres -d app_db -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog','information_schema','pg_toast') ORDER BY schema_name;"

Write-Host "`n  Done. Start services in order: dev-profile  then  dev-all" -ForegroundColor Green
