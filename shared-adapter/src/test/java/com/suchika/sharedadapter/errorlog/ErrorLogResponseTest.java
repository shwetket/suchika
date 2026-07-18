package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the security fix required by the 2026-07-13 ADR-023 revision:
 * {@code details} must be truncated before it reaches the wire, since these
 * endpoints have no {@code @RolesAllowed} (ADR-005).
 */
class ErrorLogResponseTest {

    @Test
    void from_shortDetails_passedThroughUnchanged() {
        ErrorLog log = ErrorLog.builder()
                .errorCode("NOT_FOUND")
                .httpStatus(404)
                .message("not found")
                .details("short detail")
                .build();

        ErrorLogResponse response = ErrorLogResponse.from(log);

        assertEquals("short detail", response.details);
    }

    @Test
    void from_nullDetails_staysNull() {
        ErrorLog log = ErrorLog.builder()
                .errorCode("NOT_FOUND")
                .httpStatus(404)
                .message("not found")
                .build();

        ErrorLogResponse response = ErrorLogResponse.from(log);

        assertNull(response.details);
    }

    @Test
    void from_detailsOverCap_isTruncatedWithSuffix() {
        String longDetails = "x".repeat(500);
        ErrorLog log = ErrorLog.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .httpStatus(500)
                .message("boom")
                .details(longDetails)
                .build();

        ErrorLogResponse response = ErrorLogResponse.from(log);

        assertTrue(response.details.length() < longDetails.length());
        assertTrue(response.details.endsWith("...[truncated]"));
        assertTrue(response.details.startsWith("x".repeat(200)));
    }

    @Test
    void from_mapsAllOtherFieldsUnchanged() {
        ErrorLog log = ErrorLog.builder()
                .errorCode("BAD_REQUEST")
                .httpStatus(400)
                .message("bad request")
                .build();

        ErrorLogResponse response = ErrorLogResponse.from(log);

        assertEquals("BAD_REQUEST", response.errorCode);
        assertEquals(400, response.httpStatus);
        assertEquals("bad request", response.message);
    }
}
