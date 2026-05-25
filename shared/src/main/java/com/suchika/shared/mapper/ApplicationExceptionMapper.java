package com.suchika.shared.mapper;

import com.suchika.shared.dto.ErrorResponse;
import com.suchika.shared.exception.ApplicationException;
import com.suchika.shared.logging.AppLogger;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception mapper for ApplicationException.
 * Converts application exceptions to proper HTTP responses with ErrorResponse DTO.
 * Registered as a provider so Quarkus automatically uses it for all ApplicationException instances.
 */
@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    @Override
    public Response toResponse(ApplicationException exception) {
        int statusCode = exception.getStatusCode();
        ErrorResponse errorResponse = new ErrorResponse(
            statusCode,
            exception.getErrorCode(),
            exception.getMessage(),
            exception.getDetails()
        );

        AppLogger.warn("Application error: [%s] %s", exception.getErrorCode(), exception.getMessage());

        return Response
            .status(statusCode)
            .entity(errorResponse)
            .build();
    }
}
