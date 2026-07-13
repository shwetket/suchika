package com.suchika.health.adapters.service;

import com.suchika.health.domain.ErrorLog;
import com.suchika.health.ports.input.ErrorLogUseCase;
import com.suchika.health.ports.output.ErrorLogRepository;
import com.suchika.shared.exception.ErrorLogRecorder;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;

/**
 * Backs both the domain-owned read use case ({@link ErrorLogUseCase}, exposed
 * via {@code GET /v1/errors}) and the shared write hook ({@link ErrorLogRecorder},
 * invoked by {@code ApplicationExceptionMapper} for every ApplicationException
 * that reaches it) -- both sides of the same {@code profile.error_log} table
 * (Phase 4 Application Console, ADR-023).
 */
@ApplicationScoped
public class ErrorLogService implements ErrorLogUseCase, ErrorLogRecorder {

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Override
    public void record(String errorCode, int httpStatus, String message, String details) {
        errorLogRepository.save(errorCode, httpStatus, message, details);
    }

    @Override
    public List<ErrorLog> listErrors(Instant since, int limit) {
        return errorLogRepository.findSince(since, limit);
    }
}
