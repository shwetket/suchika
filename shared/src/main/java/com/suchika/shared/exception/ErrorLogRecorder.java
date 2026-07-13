package com.suchika.shared.exception;

/**
 * Optional CDI hook that lets a domain's adapters module persist every
 * {@link ApplicationException} that reaches {@link com.suchika.shared.mapper.ApplicationExceptionMapper}
 * into that domain's own {@code error_log} table (Phase 4 Application Console, ADR-023).
 *
 * <p>{@code shared/} stays domain-agnostic (ArchUnit: shared must not depend on
 * any domain module) — this is only an interface here. Each of the four domain
 * adapters modules (profile/wealth/health/household) supplies its own
 * {@code @ApplicationScoped} implementation backed by its own {@code error_log}
 * table (ADR-003: no shared/cross-domain error table). {@code web-gateway} has
 * no database of its own (ADR-002/ADR-013) and registers no implementation —
 * {@link ApplicationExceptionMapper} injects this via CDI {@code Instance<T>}
 * so the injection point is always satisfiable even when zero beans implement
 * it (mirrors the same {@code Instance<T>} pattern already used by
 * {@code com.suchika.gateway.projection.DashboardSnapshotRepository}).
 *
 * <p>Mirrors the persist-then-rethrow pattern {@code CsvParseException}
 * already uses in wealth (ADR-014): here the "persist" step happens in the
 * shared exception mapper itself (the one place every domain's exceptions
 * already pass through), and there is nothing to rethrow — the mapper still
 * returns the same HTTP response as before, persistence is a side effect.
 */
public interface ErrorLogRecorder {

    /**
     * Persist one application error. Called after the response body has
     * already been built and logged — a persistence failure here must never
     * be allowed to change the HTTP response the caller sees.
     */
    void record(String errorCode, int httpStatus, String message, String details);
}
