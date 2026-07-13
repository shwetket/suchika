package com.suchika.gateway.console;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors the JSON shape {@code scripts/service-registry.ps1}/{@code .sh}
 * already write to {@code ~/.suchika/run/<service>.pid}:
 * {@code {pid, processName, port, service, startedAt}}.
 *
 * <p>{@code @JsonProperty} on the snake_case-serialized fields matches this
 * project's existing HTTP response convention (e.g. {@code UploadErrorLogResponse});
 * the PID-registry file itself stays camelCase (unrelated internal script format).
 */
public record PidRecord(
        long pid,
        @JsonProperty("process_name") String processName,
        int port,
        String service,
        @JsonProperty("started_at") String startedAt) {
}
