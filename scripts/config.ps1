#Requires -Version 5.1
# Single source of truth loader for Suchika PowerShell scripts.
# Reads scripts/services.json and exposes it as variables/functions.
# Dot-sourced by dev-aliases.ps1, dev-service.ps1, stop-all.ps1, health-check.ps1,
# check-prerequisites.ps1 -- not intended to be run directly.
#
# Usage (from another script in scripts/):
#   . "$PSScriptRoot\config.ps1"
#   Get-SuchikaService -Name profile   # -> port, schema, gradleModule, devTask, ...
#   $SuchikaDb.passwordFallback

$_ConfigScriptsDir = $PSScriptRoot
$_ServicesJsonPath = Join-Path $_ConfigScriptsDir 'services.json'

if (-not (Test-Path $_ServicesJsonPath)) {
    throw "scripts/services.json not found at $_ServicesJsonPath -- single source of truth for ports/schemas is missing."
}

$SuchikaConfig        = Get-Content -Raw -Path $_ServicesJsonPath | ConvertFrom-Json
$SuchikaServices      = $SuchikaConfig.services
$SuchikaDb            = $SuchikaConfig.database
$SuchikaVersionFloors = $SuchikaConfig.versionFloors
$SuchikaPidDir        = $SuchikaConfig.pidDir -replace '^~', $HOME
$SuchikaLogDir        = $SuchikaConfig.logDir -replace '^~', $HOME

$null = New-Item -ItemType Directory -Force -Path $SuchikaPidDir -ErrorAction SilentlyContinue
$null = New-Item -ItemType Directory -Force -Path $SuchikaLogDir -ErrorAction SilentlyContinue

function Get-SuchikaService {
    # Looks up one service record by name. Throws on an unknown name so callers
    # fail fast instead of silently operating on $null.
    param([Parameter(Mandatory)][string]$Name)
    $svc = $SuchikaServices | Where-Object { $_.name -eq $Name }
    if (-not $svc) { throw "Unknown service '$Name' -- check scripts/services.json" }
    return $svc
}

function Get-SuchikaServiceNames {
    # All service names in startOrder. -BackendOnly excludes the frontend ('web').
    param([switch]$BackendOnly)
    $list = $SuchikaServices
    if ($BackendOnly) { $list = $list | Where-Object { $_.kind -eq 'backend' } }
    return ($list | Sort-Object startOrder | Select-Object -ExpandProperty name)
}

function Get-SuchikaHealthUrl {
    # Uses 127.0.0.1, not "localhost" -- confirmed during testing that .NET/
    # Invoke-WebRequest can resolve "localhost" to ::1 first, and since every
    # backend here binds IPv4-only (127.0.0.1), the IPv6 loopback attempt doesn't
    # fail fast in some environments -- it hangs until the request timeout, so
    # Wait-SuchikaHealthy always reports the service unhealthy even when it's
    # actually up. 127.0.0.1 sidesteps DNS/address-family selection entirely.
    param([Parameter(Mandatory)][string]$Name)
    $svc = Get-SuchikaService -Name $Name
    return "http://127.0.0.1:$($svc.port)$($svc.healthPath)"
}

function Wait-SuchikaHealthy {
    # Polls a service's health URL until it returns HTTP 200 (and, for services with
    # a real /q/health body, "status": "UP") or $TimeoutSec elapses. Works for both
    # backend services (JSON body with a status field) and the frontend (plain HTML,
    # no body to parse -- HTTP 200 alone is treated as ready). Shared by dev-all and
    # run-local.ps1 so the "is it actually up" definition doesn't drift between them.
    param(
        [Parameter(Mandatory)][string]$Name,
        [int]$TimeoutSec = 90
    )
    $url = Get-SuchikaHealthUrl -Name $Name
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -eq 200) {
                $body = $null
                try { $body = $r.Content | ConvertFrom-Json } catch { }
                if (-not $body -or -not $body.status -or $body.status -eq 'UP') {
                    return $true
                }
            }
        } catch { }
        Start-Sleep -Seconds 2
    }
    return $false
}
