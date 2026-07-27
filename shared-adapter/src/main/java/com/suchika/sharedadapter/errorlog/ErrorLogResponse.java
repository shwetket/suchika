package com.suchika.sharedadapter.errorlog;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.shared.errorlog.ErrorLog;

import java.time.Instant;

/**
 * Wire DTO for {@code GET /v1/errors}, shared across all four domains
 * (2026-07-13 ADR-023 revision -- previously one byte-for-byte-identical
 * copy per domain).
 *
 * <p><b>Security fix (ADR-023 revision):</b> {@code details} is truncated
 * before it reaches the wire. These endpoints have no {@code @RolesAllowed}
 * (consistent with ADR-005's deferred-auth stance -- not a new gap), so
 * anything echoed back here is visible to any caller that can reach the
 * port directly. The DB column stores up to 1000 chars for full internal
 * diagnostics (unaffected by this cap -- storage vs. wire shape are
 * separate concerns); the wire response caps at a much shorter length so a
 * stack-trace-shaped exception message doesn't hand a remote caller a
 * detailed map of internal implementation details. Enforced once here, at
 * the single choke point every domain's response now shares, instead of
 * needing the same fix copy-pasted four times.
 */
public class ErrorLogResponse {

    private static final int MAX_DETAILS_LENGTH = 200;
    private static final String TRUNCATION_SUFFIX = "...[truncated]";

    @JsonProperty("error_code")
    public String errorCode;

    @JsonProperty("http_status")
    public int httpStatus;

    public String message;

    public String details;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static ErrorLogResponse from(ErrorLog log) {
        ErrorLogResponse r = new ErrorLogResponse();
        r.errorCode = log.getErrorCode();
        r.httpStatus = log.getHttpStatus();
        r.message = log.getMessage();
        r.details = truncateDetails(log.getDetails());
        r.createdAt = log.getCreatedAt();
        return r;
    }

    private static String truncateDetails(String details) {
        if (details == null || details.length() <= MAX_DETAILS_LENGTH) {
            return details;
        }
        return details.substring(0, MAX_DETAILS_LENGTH) + TRUNCATION_SUFFIX;
    }
}
