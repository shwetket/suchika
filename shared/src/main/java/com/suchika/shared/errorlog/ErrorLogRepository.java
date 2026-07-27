package com.suchika.shared.errorlog;

import java.time.Instant;
import java.util.List;

/**
 * Output port for a domain's own {@code error_log} table (ADR-023). Moved
 * here from each domain's own {@code ports.output} package (2026-07-13
 * ADR-023 revision) -- shared across all four domains verbatim. Each
 * domain's own {@code adapters} module still supplies the concrete
 * Panache-backed implementation against its own schema (ADR-003/ADR-006:
 * no cross-domain table, no shared query implementation).
 */
public interface ErrorLogRepository {

    void save(String errorCode, int httpStatus, String message, String details);

    List<ErrorLog> findSince(Instant since, int limit);
}
