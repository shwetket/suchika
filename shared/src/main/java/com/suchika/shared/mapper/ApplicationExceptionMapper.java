package com.suchika.shared.mapper;

import com.suchika.shared.dto.ErrorResponse;
import com.suchika.shared.exception.ApplicationException;
import com.suchika.shared.exception.ErrorLogRecorder;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception mapper for ApplicationException.
 * Converts application exceptions to proper HTTP responses with ErrorResponse DTO.
 * Registered as a provider so Quarkus automatically uses it for all ApplicationException instances.
 *
 * <p>Phase 4 Application Console (ADR-023): after logging, every exception is
 * also offered to an optional {@link ErrorLogRecorder} bean (persist-then-continue,
 * mirroring ADR-014's persist-then-rethrow pattern — here there is nothing to
 * rethrow, the same response is still returned either way). {@code Instance<T>}
 * keeps the injection point satisfiable when no domain registers a recorder
 * (web-gateway has no DB) — see {@link ErrorLogRecorder} javadoc.
 */
@ApplicationScoped
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    private final Instance<ErrorLogRecorder> errorLogRecorders;

    /** Plain no-arg constructor kept for direct {@code new} instantiation in unit tests (no CDI container). */
    public ApplicationExceptionMapper() {
        this.errorLogRecorders = null;
    }

    @Inject
    public ApplicationExceptionMapper(Instance<ErrorLogRecorder> errorLogRecorders) {
        this.errorLogRecorders = errorLogRecorders;
    }

    @Override
    public Response toResponse(ApplicationException exception) {
        int statusCode = exception.getStatusCode();
        ErrorResponse errorResponse = new ErrorResponse(
            statusCode,
            exception.getErrorCode(),
            exception.getMessage(),
            exception.getDetails()
        );

        if (statusCode >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            AppLogger.error("Application error: [%s] %s", exception.getErrorCode(), exception.getMessage());
        } else {
            AppLogger.warn("Application error: [%s] %s", exception.getErrorCode(), exception.getMessage());
        }

        recordError(exception, statusCode);

        return Response
            .status(statusCode)
            .entity(errorResponse)
            .build();
    }

    private void recordError(ApplicationException exception, int statusCode) {
        if (errorLogRecorders == null || errorLogRecorders.isUnsatisfied()) {
            return;
        }
        try {
            errorLogRecorders.get().record(exception.getErrorCode(), statusCode, exception.getMessage(), exception.getDetails());
        } catch (RuntimeException recordingFailure) {
            // Persisting the error log must never change the HTTP response already
            // built above — log and move on.
            AppLogger.warn("Failed to persist error_log entry: %s", recordingFailure.getMessage());
        }
    }
}
