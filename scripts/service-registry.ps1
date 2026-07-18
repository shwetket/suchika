#Requires -Version 5.1
# PID-file service registry -- the shared primitive dev-service.ps1, stop-all.ps1
# and health-check.ps1 all build on, and that later phases (simplified local run,
# Application Console) will reuse. Dot-sourced, not run directly.
#
# Design note: dev-service.ps1 launches services in a NEW GUI TERMINAL WINDOW
# (wt.exe / powershell.exe), so the PID Start-Process returns is the terminal's
# PID, not the actual java.exe/node.exe server process -- killing it would leave
# the server running. Instead, Register-SuchikaServiceAsync polls the service's
# port in a background job until it starts LISTENing, then resolves the REAL
# owning process (java.exe for quarkusDev, node.exe for npm start) via
# Get-NetTCPConnection and persists that PID. This returns immediately so
# dev-all's startup loop isn't blocked waiting on each service one at a time.
#
# PID files: <SuchikaPidDir>\<service>.pid, JSON: { pid, processName, port, service, startedAt }

. "$PSScriptRoot\config.ps1"

function Register-SuchikaServiceAsync {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][int]$Port,
        [int]$TimeoutSec = 120
    )
    $pidFile = Join-Path $SuchikaPidDir "$Name.pid"
    Start-Job -Name "suchika-pidwatch-$Name" -ScriptBlock {
        param($Name, $Port, $PidFile, $TimeoutSec)
        $deadline = (Get-Date).AddSeconds($TimeoutSec)
        while ((Get-Date) -lt $deadline) {
            $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($conn) {
                $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
                if ($proc) {
                    $record = [pscustomobject]@{
                        pid         = $proc.Id
                        processName = $proc.ProcessName
                        port        = $Port
                        service     = $Name
                        startedAt   = (Get-Date -Format 'o')
                    }
                    $record | ConvertTo-Json | Set-Content -Path $PidFile -Encoding UTF8
                    return
                }
            }
            Start-Sleep -Seconds 2
        }
        # Timed out without ever seeing the port bound -- leave no stale pid file.
    } -ArgumentList $Name, $Port, $pidFile, $TimeoutSec | Out-Null
}

function Get-SuchikaRunningPid {
    # Returns the pid-file record for $Name if the process it names is still
    # alive AND still has the same process name (guards against PID reuse by
    # an unrelated process after the original service exited). Returns $null
    # and deletes the pid file if it's missing, unreadable, or stale -- callers
    # should fall back to today's port-based detection in that case.
    param([Parameter(Mandatory)][string]$Name)
    $pidFile = Join-Path $SuchikaPidDir "$Name.pid"
    if (-not (Test-Path $pidFile)) { return $null }
    try {
        $record = Get-Content -Raw -Path $pidFile | ConvertFrom-Json
    } catch {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        return $null
    }
    $proc = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    if (-not $proc -or $proc.ProcessName -ne $record.processName) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        return $null
    }
    return $record
}

function Remove-SuchikaServicePid {
    param([Parameter(Mandatory)][string]$Name)
    $pidFile = Join-Path $SuchikaPidDir "$Name.pid"
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}
