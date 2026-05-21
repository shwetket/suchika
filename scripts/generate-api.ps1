#Requires -Version 5.1
# Regenerates web/src/api/generated.ts from application/contract/gateway.yaml.
# Run after ANY change to a backend OpenAPI contract.
# Usage: .\scripts\generate-api.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Write-Host "`n==> Regenerating OpenAPI client  →  web/src/api/generated.ts" -ForegroundColor Cyan
$start = Get-Date

Push-Location (Join-Path $root 'web')
try {
    & npm run generate:api
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

$elapsed = [int]([datetime]::Now - $start).TotalSeconds

if ($exit -eq 0) {
    Write-Host "  [OK]  generated.ts updated  ($($elapsed)s)" -ForegroundColor Green
    Write-Host "        Never hand-edit web/src/api/generated.ts — always run generate-api." -ForegroundColor DarkGray
} else {
    Write-Host "  [X]   Generation failed. Is the gateway contract at application/contract/gateway.yaml valid?" -ForegroundColor Red
    exit $exit
}
