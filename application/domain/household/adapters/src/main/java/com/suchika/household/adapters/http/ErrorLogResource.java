package com.suchika.household.adapters.http;

import com.suchika.household.adapters.http.dto.ErrorLogResponse;
import com.suchika.household.ports.input.ErrorLogUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Phase 4 Application Console (ADR-023): exposes this domain's own
 * {@code error_log} table for the admin-only console to read. Mirrors the
 * shape of the existing {@code GET /v1/accounts/{id}/uploads/{id}/errors} in
 * wealth. No profile_id scoping (ADR-006 governs member-owned business data;
 * this is an operational/audit log, same precedent as wealth.upload_error_log).
 */
@Path("/v1/errors")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorLogResource {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    private final ErrorLogUseCase errorLogUseCase;

    public ErrorLogResource(ErrorLogUseCase errorLogUseCase) {
        this.errorLogUseCase = errorLogUseCase;
    }

    @GET
    public List<ErrorLogResponse> listErrors(
            @QueryParam("since") String since,
            @QueryParam("limit") Integer limit) {
        Instant sinceInstant = parseSince(since);
        int effectiveLimit = effectiveLimit(limit);
        return errorLogUseCase.listErrors(sinceInstant, effectiveLimit)
                .stream()
                .map(ErrorLogResponse::from)
                .toList();
    }

    private Instant parseSince(String since) {
        if (since == null || since.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(since);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("since must be an ISO-8601 instant, e.g. 2026-07-13T00:00:00Z");
        }
    }

    private int effectiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
