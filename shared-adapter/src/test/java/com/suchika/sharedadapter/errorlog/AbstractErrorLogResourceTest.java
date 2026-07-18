package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogUseCase;
import com.suchika.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Full since/limit-parsing and pagination coverage for {@link
 * AbstractErrorLogResource}, exercised through a minimal concrete test
 * subclass -- pure Java, no Quarkus context needed. Replaces what were four
 * near-identical per-domain {@code ErrorLogResourceTest} classes (2026-07-13
 * ADR-023 revision); each domain's own {@code ErrorLogResourceTest} is now a
 * thin wiring smoke test only.
 */
class AbstractErrorLogResourceTest {

    private TestResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new TestResource(useCase);
    }

    @Test
    void listErrors_noParams_usesDefaultLimitAndNullSince() {
        useCase.toReturn = List.of(buildErrorLog());

        List<ErrorLogResponse> response = resource.listErrors(null, null);

        assertEquals(1, response.size());
        assertEquals("NOT_FOUND", response.get(0).errorCode);
        assertNull(useCase.lastSince);
        assertEquals(50, useCase.lastLimit);
    }

    @Test
    void listErrors_validSince_parsesInstant() {
        useCase.toReturn = List.of();

        resource.listErrors("2026-07-01T00:00:00Z", 10);

        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), useCase.lastSince);
        assertEquals(10, useCase.lastLimit);
    }

    @Test
    void listErrors_invalidSince_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listErrors("not-a-date", null));
    }

    @Test
    void listErrors_limitAboveMax_isCapped() {
        useCase.toReturn = List.of();

        resource.listErrors(null, 10_000);

        assertEquals(500, useCase.lastLimit);
    }

    @Test
    void listErrors_negativeLimit_fallsBackToDefault() {
        useCase.toReturn = List.of();

        resource.listErrors(null, -5);

        assertEquals(50, useCase.lastLimit);
    }

    private ErrorLog buildErrorLog() {
        return ErrorLog.builder()
                .errorCode("NOT_FOUND")
                .httpStatus(404)
                .message("Profile not found")
                .build();
    }

    static class TestResource extends AbstractErrorLogResource {
        private final ErrorLogUseCase useCase;

        TestResource(ErrorLogUseCase useCase) {
            this.useCase = useCase;
        }

        @Override
        protected ErrorLogUseCase useCase() {
            return useCase;
        }
    }

    static class StubUseCase implements ErrorLogUseCase {
        List<ErrorLog> toReturn = List.of();
        Instant lastSince;
        int lastLimit;

        @Override
        public List<ErrorLog> listErrors(Instant since, int limit) {
            lastSince = since;
            lastLimit = limit;
            return toReturn;
        }
    }
}
