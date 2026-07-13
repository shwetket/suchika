package com.suchika.household.adapters.http;

import com.suchika.household.adapters.http.dto.ErrorLogResponse;
import com.suchika.household.domain.ErrorLog;
import com.suchika.household.ports.input.ErrorLogUseCase;
import com.suchika.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorLogResourceTest {

    private ErrorLogResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new ErrorLogResource(useCase);
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
