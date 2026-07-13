package com.suchika.gateway.console;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.InternalServerException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Starts/stops one Suchika service for the Application Console (Phase 4,
 * ADR-023) by shelling out to {@code scripts/run-local.ps1}/{@code .sh} and
 * {@code scripts/stop-local.ps1}/{@code .sh} with the per-service targeting
 * added in this same phase (Part A) — reused here, not reimplemented, exactly
 * as the CsvParseException/upload_error_log precedent (ADR-014) reuses
 * existing machinery rather than duplicating it.
 *
 * <p>OS detection: {@code os.name} system property, the same signal
 * {@code check-prerequisites.ps1}/{@code .sh} and this project's Codespaces
 * vs Windows split are built around ({@code documents/SCRIPTS.md}) — there is
 * no prior Java-side OS-detection code in this repo to mirror, since every
 * other cross-platform decision here is made by shipping a separate
 * {@code .ps1}/{@code .sh} file rather than branching at runtime.
 */
@ApplicationScoped
public class ServiceControlService {

    private static final int PROCESS_WAIT_TIMEOUT_SECONDS = 90;
    private static final String STATUS_OK = "OK";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_TIMEOUT = "TIMEOUT";

    private final Optional<String> repoRootOverride;

    public ServiceControlService(
            @ConfigProperty(name = "suchika.repo-root") Optional<String> repoRootOverride) {
        this.repoRootOverride = repoRootOverride;
    }

    public ServiceActionResult start(String service) {
        return runScript(service, "start", "run-local");
    }

    public ServiceActionResult stop(String service) {
        return runScript(service, "stop", "stop-local");
    }

    private ServiceActionResult runScript(String service, String action, String scriptBaseName) {
        if (!ServiceDefinition.isKnown(service)) {
            throw new BadRequestException("Unknown service '" + service + "' -- check scripts/services.json");
        }
        Path repoRoot = resolveRepoRoot();
        ProcessBuilder processBuilder = buildProcess(repoRoot, scriptBaseName, service);
        processBuilder.directory(repoRoot.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            AppLogger.info("Console: running %s for service %s", scriptBaseName, service);
            Process process = processBuilder.start();
            String output = readOutput(process);
            boolean finished = process.waitFor(PROCESS_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                return new ServiceActionResult(service, action, STATUS_TIMEOUT,
                        output + "\n(script still running after " + PROCESS_WAIT_TIMEOUT_SECONDS
                                + "s -- check GET /v1/console/status separately)");
            }
            String status = process.exitValue() == 0 ? STATUS_OK : STATUS_FAILED;
            return new ServiceActionResult(service, action, status, output);
        } catch (IOException e) {
            throw new InternalServerException("Failed to run " + scriptBaseName + " for " + service + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("Interrupted while running " + scriptBaseName + " for " + service);
        }
    }

    private ProcessBuilder buildProcess(Path repoRoot, String scriptBaseName, String service) {
        if (isWindows()) {
            Path script = repoRoot.resolve("scripts").resolve(scriptBaseName + ".ps1");
            return new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-Service", service);
        }
        Path script = repoRoot.resolve("scripts").resolve(scriptBaseName + ".sh");
        return new ProcessBuilder("bash", script.toString(), service);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String readOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * Walks up from the JVM's working directory looking for {@code scripts/services.json}
     * (the same marker file {@code config.ps1}/{@code config.sh} treat as the single
     * source of truth) so this works whether Gradle's cwd for {@code quarkusDev} is
     * the repo root or the {@code application/web-gateway} module directory.
     * Override with {@code suchika.repo-root} if neither applies (e.g. a packaged jar).
     */
    private Path resolveRepoRoot() {
        if (repoRootOverride.isPresent()) {
            return Path.of(repoRootOverride.get());
        }
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("scripts").resolve("services.json"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new InternalServerException(
                "Could not locate the Suchika repo root (scripts/services.json) from " + Path.of("").toAbsolutePath()
                        + " -- set suchika.repo-root explicitly if running outside a repo checkout");
    }
}
