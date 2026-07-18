#Requires -Version 5.1
# Stops services started by run-local.ps1 (headless mode).
#
# This is a thin, deliberately-named wrapper around stop-all.ps1, not a second
# implementation: stop-all.ps1 already checks the PID-file registry first (Phase 1's
# service-registry.ps1) and falls back to port-based kill only if no PID record
# exists -- and that logic is identical regardless of whether a service was started
# in a GUI window (dev-service.ps1) or headlessly (run-local.ps1), because both
# register into the exact same ~/.suchika/run/<service>.pid files. There is nothing
# headless-specific left to write.
#
# Kept as its own script/alias (stop-local / sl) instead of just telling users to run
# stop-all/sa because run-local/stop-local reads as a matched pair next to
# dev-all/stop-all, and it leaves a clean seam to grow into if headless-mode stop
# semantics ever need to diverge from the GUI-window workflow's.
#
# Usage: .\scripts\stop-local.ps1
& "$PSScriptRoot\stop-all.ps1" @args
