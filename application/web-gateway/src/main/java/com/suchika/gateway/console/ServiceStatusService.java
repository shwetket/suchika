package com.suchika.gateway.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Live status for the Application Console (Phase 4, ADR-023): polls each
 * service's real {@code /q/health} (same "HTTP 200 + status: UP" definition
 * already established by {@code scripts/config.ps1}'s {@code Wait-SuchikaHealthy}
 * / {@code config.sh}'s {@code suchika_wait_healthy}) and reads the PID-registry
 * file each service's start script already writes to
 * {@code ~/.suchika/run/<service>.pid}.
 *
 * <p>Uses {@code 127.0.0.1}, not {@code localhost} — mirrors the fix already
 * applied to {@code config.ps1}/{@code config.sh} (Phase 3): {@code localhost}
 * can resolve to {@code ::1} first and hang against an IPv4-only bind.
 */
@ApplicationScoped
public class ServiceStatusService {

    private static final int HEALTH_TIMEOUT_SECONDS = 2;
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HEALTH_TIMEOUT_SECONDS))
            .build();

    public List<ServiceStatus> listStatuses() {
        return ServiceDefinition.getAll().stream()
                .map(this::statusOf)
                .toList();
    }

    private ServiceStatus statusOf(ServiceDefinition def) {
        String status = isHealthy(def) ? STATUS_UP : STATUS_DOWN;
        PidRecord pid = readPidFile(def.name());
        return new ServiceStatus(def.name(), def.port(), def.kind(), status, pid);
    }

    private boolean isHealthy(ServiceDefinition def) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + def.port() + def.healthPath()))
                    .timeout(Duration.ofSeconds(HEALTH_TIMEOUT_SECONDS))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && bodyReportsUp(response.body());
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean bodyReportsUp(String body) {
        if (body == null || body.isBlank()) {
            // The frontend's "/" has no JSON health body — HTTP 200 alone means UP.
            return true;
        }
        try {
            JsonNode json = MAPPER.readTree(body);
            JsonNode statusNode = json.get("status");
            return statusNode == null || STATUS_UP.equals(statusNode.asText());
        } catch (IOException notJson) {
            // Non-JSON 200 body (e.g. the frontend's HTML) still counts as UP.
            return true;
        }
    }

    private PidRecord readPidFile(String name) {
        Path pidFile = Path.of(System.getProperty("user.home"), ".suchika", "run", name + ".pid");
        if (!Files.exists(pidFile)) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(Files.readString(pidFile));
            return new PidRecord(
                    node.path("pid").asLong(),
                    node.path("processName").asText(null),
                    node.path("port").asInt(),
                    node.path("service").asText(null),
                    node.path("startedAt").asText(null));
        } catch (IOException e) {
            AppLogger.warn("Could not read pid registry file for %s: %s", name, e.getMessage());
            return null;
        }
    }
}
