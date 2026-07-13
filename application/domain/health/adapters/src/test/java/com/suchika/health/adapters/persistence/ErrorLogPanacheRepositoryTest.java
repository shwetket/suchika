package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.ErrorLog;
import com.suchika.health.ports.output.ErrorLogRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for ErrorLogPanacheRepository. Requires a running local
 * PostgreSQL (app_db) -- runs against the shared %integration-test profile,
 * same pattern every other domain's adapter tests already use (Testcontainers
 * is tracked but unimplemented project-wide as of the 2026-07-06 retrospective).
 */
@QuarkusTest
@TestProfile(ErrorLogPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class ErrorLogPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    @Inject
    ErrorLogRepository errorLogRepository;

    @Test
    void save_andFindSince_roundTrip() {
        errorLogRepository.save("NOT_FOUND", 404, "Profile not found", "ID: 12345");

        List<ErrorLog> results = errorLogRepository.findSince(null, 50);

        assertTrue(results.stream().anyMatch(e -> "Profile not found".equals(e.getMessage())));
        ErrorLog log = results.stream().filter(e -> "Profile not found".equals(e.getMessage())).findFirst().orElseThrow();
        assertNotNull(log.getId());
        assertEquals("NOT_FOUND", log.getErrorCode());
        assertEquals(404, log.getHttpStatus());
        assertEquals("ID: 12345", log.getDetails());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void findSince_futureTimestamp_excludesExistingRows() {
        errorLogRepository.save("BAD_REQUEST", 400, "bad request", null);
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

        List<ErrorLog> results = errorLogRepository.findSince(future, 50);

        assertTrue(results.isEmpty());
    }

    @Test
    void findSince_respectsLimit() {
        for (int i = 0; i < 5; i++) {
            errorLogRepository.save("INTERNAL_SERVER_ERROR", 500, "failure " + i, null);
        }

        List<ErrorLog> results = errorLogRepository.findSince(null, 2);

        assertEquals(2, results.size());
    }
}
