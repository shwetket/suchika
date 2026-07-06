#Requires -Version 5.1
# Starts the local SonarQube server and opens the dashboard in a browser.
# SonarQube must be extracted in the repo root as sonarqube-*/.
# Usage: .\scripts\sonar-start.ps1

$root = Split-Path -Parent $PSScriptRoot

# ── Find SonarQube installation ────────────────────────────────────────────────
$sonarDir = Get-ChildItem $root -Directory -Filter 'sonarqube-*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1

if (-not $sonarDir) {
    Write-Host "  [X]  No sonarqube-* directory found in repo root." -ForegroundColor Red
    Write-Host "       Download from: https://www.sonarsource.com/products/sonarqube/downloads/" -ForegroundColor Yellow
    Write-Host "       Extract to: $root\sonarqube-X.Y.Z\" -ForegroundColor Yellow
    exit 1
}

$startBat = Join-Path $sonarDir.FullName 'bin\windows-x86-64\StartSonar.bat'
if (-not (Test-Path $startBat)) {
    Write-Host "  [X]  StartSonar.bat not found at: $startBat" -ForegroundColor Red
    exit 1
}

# ── Check if already running ───────────────────────────────────────────────────
$alreadyUp = $false
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:9000/api/system/status' -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
    $alreadyUp = ($r.StatusCode -eq 200)
} catch { $alreadyUp = $false }

if ($alreadyUp) {
    Write-Host "  [OK]  SonarQube already running at http://localhost:9000" -ForegroundColor Green
    Start-Process 'http://localhost:9000'
    return
}

# ── Start SonarQube in a new window ───────────────────────────────────────────
Write-Host "`n==> Starting SonarQube ($($sonarDir.Name))" -ForegroundColor Cyan
Start-Process cmd.exe -ArgumentList '/k', "`"$startBat`""

# ── Poll until ready (max 90s) ─────────────────────────────────────────────────
Write-Host "     Waiting for SonarQube to be ready..." -ForegroundColor DarkGray
$deadline = (Get-Date).AddSeconds(90)
$ready    = $false

while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:9000/api/system/status' -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($r.StatusCode -eq 200) {
            $json = $r.Content | ConvertFrom-Json
            if ($json.status -eq 'UP') { $ready = $true; break }
        }
    } catch { }
    Write-Host '.' -NoNewline -ForegroundColor DarkGray
}

Write-Host ''

if ($ready) {
    Write-Host "  [OK]  SonarQube is UP  →  http://localhost:9000" -ForegroundColor Green
    Write-Host "        Default login: admin / admin  (change on first login)" -ForegroundColor DarkGray
    Start-Process 'http://localhost:9000'
} else {
    Write-Host "  [!]  SonarQube did not respond within 90s." -ForegroundColor Yellow
    Write-Host "       Check the console window for errors, then open: http://localhost:9000" -ForegroundColor Yellow
}
