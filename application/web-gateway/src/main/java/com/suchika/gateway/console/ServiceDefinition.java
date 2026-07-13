package com.suchika.gateway.console;

import java.util.List;

/**
 * Static description of one Suchika service for the Application Console
 * (Phase 4, ADR-023). Ports/names mirror {@code scripts/services.json} and
 * the fixed port policy in {@code CLAUDE.md} ("Service ports ... Do not
 * change") — duplicated here deliberately rather than read from the JSON
 * file at runtime, since the gateway is a compiled artifact that may not
 * always run with the repo checkout on disk (e.g. a container image), while
 * {@link ServiceControlService} (which DOES need the repo checkout to shell
 * out to run-local.ps1/.sh) resolves the repo root separately and can fail
 * loudly if it's missing.
 */
public record ServiceDefinition(String name, int port, String healthPath, String kind) {

    public static final List<ServiceDefinition> ALL = List.of(
            new ServiceDefinition("profile", 8081, "/q/health", "backend"),
            new ServiceDefinition("wealth", 8082, "/q/health", "backend"),
            new ServiceDefinition("health", 8083, "/q/health", "backend"),
            new ServiceDefinition("household", 8084, "/q/health", "backend"),
            new ServiceDefinition("gateway", 8080, "/q/health", "backend"),
            new ServiceDefinition("web", 3000, "/", "frontend")
    );

    public static boolean isKnown(String name) {
        return ALL.stream().anyMatch(s -> s.name().equals(name));
    }
}
