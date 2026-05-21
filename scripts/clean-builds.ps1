#Requires -Version 5.1
# Safe clean: removes Gradle build outputs, React build, and .temp logs.
# Does NOT touch node_modules, .env files, or uncommitted source files.
# Usage: .\scripts\clean-builds.ps1

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot

Write-Host "`n==> Cleaning build outputs" -ForegroundColor Cyan

Push-Location $root
try {
    Write-Host "  Gradle clean..." -ForegroundColor DarkGray
    & .\gradlew.bat clean 2>&1 | Where-Object { $_ -match 'BUILD|FAILED|ERROR|:clean' }
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK]  Gradle build/ directories removed" -ForegroundColor Green
    } else {
        Write-Host "  [!]  Gradle clean exited $LASTEXITCODE" -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}

$webBuild = Join-Path $root 'web\build'
if (Test-Path $webBuild) {
    Remove-Item $webBuild -Recurse -Force
    Write-Host "  [OK]  web/build removed" -ForegroundColor Green
}

$temp = Join-Path $root '.temp'
if (Test-Path $temp) {
    Remove-Item $temp -Recurse -Force
    Write-Host "  [OK]  .temp (build logs) removed" -ForegroundColor Green
}

Write-Host "`n  Clean complete. Run build-all or dev-{service} to rebuild." -ForegroundColor Green
