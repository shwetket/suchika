package com.suchika.household.adapters.service;

import com.suchika.shared.errorlog.ErrorLogRepository;
import com.suchika.sharedadapter.errorlog.AbstractErrorLogService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Backs both the domain-owned read use case ({@code ErrorLogUseCase},
 * exposed via {@code GET /v1/errors}) and the shared write hook ({@code
 * ErrorLogRecorder}, invoked by {@code ApplicationExceptionMapper} for
 * every ApplicationException that reaches it) -- both sides of the same
 * {@code household.error_log} table (Phase 4 Application Console, ADR-023).
 *
 * <p>Delegation logic lives once in {@link AbstractErrorLogService}
 * (2026-07-13 ADR-023 revision) -- this class only binds the domain's own
 * {@link ErrorLogRepository} implementation.
 */
@ApplicationScoped
public class ErrorLogService extends AbstractErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Override
    protected ErrorLogRepository repository() {
        return errorLogRepository;
    }
}
