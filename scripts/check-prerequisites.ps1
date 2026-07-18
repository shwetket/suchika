# Checks that all tools required to build and run Suchika are present and at the right version.
# Run before the first build or after a fresh machine setup.
# Java/Node version floors come from scripts/services.json (single source of
# truth) via config.ps1 -- do not hardcode them here.
# Usage: .\scripts\check-prerequisites.ps1
$ErrorActionPreference = "Continue"  # don't stop on individual check failures

. "$PSScriptRoot\config.ps1"
$javaFloor = $SuchikaVersionFloors.javaHardFailBelow
$javaTarget = $SuchikaVersionFloors.javaTarget
$nodeFloor = $SuchikaVersionFloors.nodeHardFailBelow
$nodeTarget = $SuchikaVersionFloors.nodeTarget

$pass = 0; $fail = 0; $warn = 0

function OK($msg)   { Write-Host "  [PASS] $msg" -ForegroundColor Green;  $script:pass++ }
function Fail($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red;    $script:fail++ }
function Warn($msg) { Write-Host "  [WARN] $msg" -ForegroundColor Yellow; $script:warn++ }

function CommandExists($cmd) {
    $null -ne (Get-Command $cmd -ErrorAction SilentlyContinue)
}

function ExtractVersion($str) {
    if ($str -match '(\d+)\.(\d+)') { return [int]$Matches[1], [int]$Matches[2] }
    return 0, 0
}

Write-Host "`n=== Suchika Prerequisite Check ===`n" -ForegroundColor Cyan

# ── Git ───────────────────────────────────────────────────────────────────────
Write-Host "Version Control" -ForegroundColor White
if (CommandExists git) {
    $v = (git --version) -replace 'git version ',''
    OK "git $v"
} else {
    Fail "git not found -- install Git from https://git-scm.com"
}

# ── Java ──────────────────────────────────────────────────────────────────────
Write-Host "`nJava (required: $javaFloor+, target: $javaTarget)" -ForegroundColor White
if (CommandExists java) {
    $javaOut = (java -version 2>&1) | Out-String
    $maj, $_ = ExtractVersion $javaOut
    if ($maj -ge $javaTarget) {
        OK "Java $maj (matches CI)"
    } elseif ($maj -ge $javaFloor) {
        Warn "Java $maj found -- CI/build.gradle.kts target Java $javaTarget; upgrade to avoid CI/local divergence"
    } else {
        Fail "Java $maj found -- Java below $javaFloor is no longer supported. Install Temurin $javaTarget`: https://adoptium.net"
    }
} else {
    Fail "java not found -- install Temurin $javaTarget`: https://adoptium.net"
}

if (CommandExists javac) {
    $jcOut = (javac -version 2>&1) | Out-String
    $maj, $_ = ExtractVersion $jcOut
    OK "javac $maj"
} else {
    Warn "javac not found -- make sure JDK (not JRE) is installed"
}

# ── Gradle wrapper ────────────────────────────────────────────────────────────
Write-Host "`nGradle" -ForegroundColor White
$root = Split-Path -Parent $PSScriptRoot
if (Test-Path "$root\gradlew.bat") {
    OK "gradlew.bat present"
} else {
    Fail "gradlew.bat not found in repo root -- repository may be incomplete"
}

# ── Node.js ───────────────────────────────────────────────────────────────────
Write-Host "`nNode.js (required: $nodeFloor+, CI uses $nodeTarget)" -ForegroundColor White
if (CommandExists node) {
    $nodeVer = (node --version) -replace 'v',''
    $maj, $_ = ExtractVersion $nodeVer
    if ($maj -ge $nodeTarget) {
        OK "Node.js $nodeVer (matches CI)"
    } elseif ($maj -ge $nodeFloor) {
        Warn "Node.js $nodeVer -- works locally, but CI uses Node $nodeTarget; upgrade if you hit CI failures"
    } else {
        Fail "Node.js $nodeVer -- need $nodeFloor+. Install: https://nodejs.org"
    }
} else {
    Fail "node not found -- install Node.js $nodeTarget LTS: https://nodejs.org"
}

# ── npm ───────────────────────────────────────────────────────────────────────
Write-Host "`nnpm" -ForegroundColor White
if (CommandExists npm) {
    $npmVer = npm --version
    OK "npm $npmVer"
} else {
    Fail "npm not found -- comes bundled with Node.js"
}

# ── PostgreSQL ────────────────────────────────────────────────────────────────
Write-Host "`nPostgreSQL" -ForegroundColor White
if (CommandExists psql) {
    $pgVer = (psql --version) -replace 'psql \(PostgreSQL\) ',''
    $maj, $_ = ExtractVersion $pgVer
    if ($maj -ge 13) {
        OK "psql $pgVer"
    } else {
        Warn "psql $pgVer -- PostgreSQL 13+ recommended"
    }
} else {
    Warn "psql not in PATH -- install PostgreSQL 15+: https://www.postgresql.org/download/windows/"
}

# Check if PostgreSQL is listening
try {
    $tc = New-Object System.Net.Sockets.TcpClient
    $tc.Connect("localhost", $SuchikaDb.port)
    $tc.Close()
    OK "PostgreSQL listening on localhost:$($SuchikaDb.port)"
} catch {
    Warn "Nothing listening on localhost:$($SuchikaDb.port) -- start PostgreSQL before running any domain service"
}

# ── Optional: curl ────────────────────────────────────────────────────────────
# Use curl.exe explicitly — PowerShell aliases 'curl' to Invoke-WebRequest which is not the same
Write-Host "`nOptional tools" -ForegroundColor White
$curlExe = Get-Command curl.exe -ErrorAction SilentlyContinue
if ($null -ne $curlExe) {
    $curlVer = (& curl.exe --version 2>&1 | Select-Object -First 1) -replace '^curl ',''
    OK "curl.exe $curlVer (used by health-check.sh)"
} else {
    Warn "curl.exe not found -- health-check.sh uses it; PowerShell health-check.ps1 does not need it"
}

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
$total = $pass + $fail + $warn
if ($fail -eq 0) {
    if ($warn -eq 0) {
        Write-Host "All $total checks passed -- environment is ready." -ForegroundColor Green
    } else {
        Write-Host "$pass passed, $warn warnings, 0 failures -- review warnings above." -ForegroundColor Yellow
    }
} else {
    Write-Host "$fail checks failed, $warn warnings -- fix failures before building." -ForegroundColor Red
    exit 1
}
Write-Host ""
