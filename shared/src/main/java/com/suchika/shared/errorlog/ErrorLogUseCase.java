package com.suchika.shared.errorlog;

import java.time.Instant;
import java.util.List;

/**
 * Read side of Phase 4's Application Console error log (ADR-023). The write
 * side is {@code com.suchika.shared.exception.ErrorLogRecorder}, implemented
 * by the same adapter service that backs this use case.
 *
 * <p>Moved here from each domain's own {@code ports.input} package
 * (2026-07-13 ADR-023 revision) -- shared across all four domains verbatim.
 * Each domain's own {@code adapters} module still supplies the concrete
 * implementation backed by its own {@code error_log} table (ADR-003).
 */
public interface ErrorLogUseCase {

    List<ErrorLog> listErrors(Instant since, int limit);
}
