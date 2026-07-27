#!/usr/bin/env bash
# PID-file service registry -- bash/Codespaces counterpart to service-registry.ps1.
# Same shared primitive: dev-aliases.sh's _dev_svc, stop-all(), and status() all
# build on this instead of each re-implementing their own PID handling.
# Source this file; do not execute it directly.
#
# PID files: $SUCHIKA_PID_DIR/<service>.pid, one JSON object per file:
#   {"pid": 1234, "processName": "java", "port": 8081, "service": "profile", "startedAt": "..."}
#
# Design note: unlike Windows (which opens a new GUI terminal, so the PID
# Start-Process sees isn't the real server process), bash's _dev_svc already
# backgrounds gradlew directly, so $! is a close ancestor of the real process --
# but still not the java/node PID itself once Gradle forks it. Rather than track
# two different kinds of "close enough" PIDs on the two platforms, both sides
# resolve and persist the SAME thing: the OS process actually LISTENing on the
# service's port, found by polling in a background subshell so callers return
# immediately (mirrors Register-SuchikaServiceAsync's Start-Job on Windows).

suchika_register_service_async() {
  local svc="$1" port="$2" timeout="${3:-120}"
  local pid_file="$SUCHIKA_PID_DIR/$svc.pid"
  (
    local waited=0 pid pname
    while (( waited < timeout )); do
      pid=$(lsof -ti tcp:"$port" 2>/dev/null | head -1)
      if [ -n "$pid" ]; then
        pname=$(ps -p "$pid" -o comm= 2>/dev/null | tr -d ' ')
        printf '{"pid": %s, "processName": "%s", "port": %s, "service": "%s", "startedAt": "%s"}\n' \
          "$pid" "$pname" "$port" "$svc" "$(date -Iseconds 2>/dev/null || date)" > "$pid_file"
        exit 0
      fi
      sleep 2
      (( waited += 2 ))
    done
  ) &
  disown 2>/dev/null || true
}

# Prints the pid if the registered process is still alive and still has the
# same command name (guards against PID reuse); returns 1 and removes the
# pid file otherwise. Callers fall back to port-based detection on failure.
suchika_get_running_pid() {
  local svc="$1" pid_file="$SUCHIKA_PID_DIR/$svc.pid"
  [[ -f "$pid_file" ]] || return 1
  local pid pname actual
  pid=$(grep -oP '"pid":\s*\K[0-9]+' "$pid_file" 2>/dev/null)
  pname=$(grep -oP '"processName":\s*"\K[^"]*' "$pid_file" 2>/dev/null)
  if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
    rm -f "$pid_file"
    return 1
  fi
  actual=$(ps -p "$pid" -o comm= 2>/dev/null | tr -d ' ')
  if [[ -n "$pname" && "$actual" != "$pname" ]]; then
    rm -f "$pid_file"
    return 1
  fi
  echo "$pid"
}

suchika_remove_service_pid() {
  local svc="$1"
  rm -f "$SUCHIKA_PID_DIR/$svc.pid"
}
