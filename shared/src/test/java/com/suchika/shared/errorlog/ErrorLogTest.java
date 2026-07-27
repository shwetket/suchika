package com.suchika.shared.errorlog;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * One shared test for the one shared {@link ErrorLog} type (2026-07-13
 * ADR-023 revision) -- replaces four byte-for-byte identical per-domain
 * {@code ErrorLogTest} classes.
 */
class ErrorLogTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-13T10:00:00Z");

        ErrorLog log = ErrorLog.builder()
                .id(id)
                .errorCode("NOT_FOUND")
                .httpStatus(404)
                .message("Profile not found")
                .details("ID: 12345")
                .createdAt(createdAt)
                .build();

        assertEquals(id, log.getId());
        assertEquals("NOT_FOUND", log.getErrorCode());
        assertEquals(404, log.getHttpStatus());
        assertEquals("Profile not found", log.getMessage());
        assertEquals("ID: 12345", log.getDetails());
        assertEquals(createdAt, log.getCreatedAt());
    }

    @Test
    void builder_noDetails_detailsIsNull() {
        ErrorLog log = ErrorLog.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .httpStatus(500)
                .message("Unexpected failure")
                .build();

        assertNull(log.getId());
        assertNull(log.getDetails());
        assertNull(log.getCreatedAt());
        assertEquals("INTERNAL_SERVER_ERROR", log.getErrorCode());
        assertEquals(500, log.getHttpStatus());
        assertEquals("Unexpected failure", log.getMessage());
    }
}
