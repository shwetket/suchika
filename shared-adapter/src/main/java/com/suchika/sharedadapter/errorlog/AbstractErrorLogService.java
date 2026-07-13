package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogRepository;
import com.suchika.shared.errorlog.ErrorLogUseCase;
import com.suchika.shared.exception.ErrorLogRecorder;

import java.time.Instant;
import java.util.List;

/**
 * Shared delegation logic backing both the domain-owned read use case
 * ({@link ErrorLogUseCase}, exposed via {@code GET /v1/errors}) and the
 * shared write hook ({@link ErrorLogRecorder}, invoked by {@code
 * ApplicationExceptionMapper} for every {@code ApplicationException} that
 * reaches it) -- extracted 2026-07-13 from four byte-for-byte-identical
 * per-domain {@code ErrorLogService} classes (Phase 4 Application Console,
 * ADR-023).
 *
 * <p>Each domain's own {@code adapters} module supplies a small concrete
 * {@code @ApplicationScoped} subclass binding its own {@link
 * ErrorLogRepository} implementation (backed by that domain's own {@code
 * error_log} table -- ADR-003/ADR-006, no shared table).
 */
public abstract class AbstractErrorLogService implements ErrorLogUseCase, ErrorLogRecorder {

    protected abstract ErrorLogRepository repository();

    @Override
    public void record(String errorCode, int httpStatus, String message, String details) {
        repository().save(errorCode, httpStatus, message, details);
    }

    @Override
    public List<ErrorLog> listErrors(Instant since, int limit) {
        return repository().findSince(since, limit);
    }
}
