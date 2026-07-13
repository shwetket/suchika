package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLogUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Phase 4 Application Console (ADR-023): shared {@code since}/{@code limit}
 * parsing and pagination logic for {@code GET /v1/errors}, extracted
 * 2026-07-13 from four byte-for-byte-identical per-domain {@code
 * ErrorLogResource} classes.
 *
 * <p>Carries no {@code @Path}/{@code @ApplicationScoped} of its own -- each
 * domain's own {@code adapters} module supplies a small concrete subclass
 * that adds those annotations and binds its own {@link ErrorLogUseCase}
 * implementation. Quarkus/RESTEasy Reactive only ever has to discover the
 * concrete subclass (already living in that domain's already-indexed
 * adapters module) -- no new cross-module Jandex indexing behavior is
 * introduced (see ADR-023's "Q2" for why the alternative, a fully shared
 * concrete {@code @Path} class, was rejected as unverified in this build).
 */
public abstract class AbstractErrorLogResource {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    protected abstract ErrorLogUseCase useCase();

    @GET
    public List<ErrorLogResponse> listErrors(
            @QueryParam("since") String since,
            @QueryParam("limit") Integer limit) {
        Instant sinceInstant = parseSince(since);
        int effectiveLimit = effectiveLimit(limit);
        return useCase().listErrors(sinceInstant, effectiveLimit)
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
