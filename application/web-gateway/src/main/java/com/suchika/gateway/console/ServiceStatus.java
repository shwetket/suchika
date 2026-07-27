package com.suchika.gateway.console;

/**
 * Live status of one service, as reported by {@link ServiceStatusService}.
 * {@code pid} is null when no PID-registry record exists for this service
 * (either never started via the {@code run-local}/{@code dev-*} scripts, or
 * the registry entry aged out — see {@code service-registry.ps1}/{@code .sh}).
 */
public record ServiceStatus(String name, int port, String kind, String status, PidRecord pid) {
}
