package com.suchika.gateway.console;

/**
 * Result of a start/stop action shelled out to run-local/stop-local (Phase 4
 * Application Console, ADR-023). {@code status} is one of OK / FAILED / TIMEOUT.
 */
public record ServiceActionResult(String service, String action, String status, String output) {
}
