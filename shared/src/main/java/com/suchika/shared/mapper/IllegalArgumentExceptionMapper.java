package com.suchika.shared.mapper;

import com.suchika.shared.dto.ErrorResponse;
import com.suchika.shared.logging.AppLogger;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Global exception mapper for IllegalArgumentException.
 * Domain-layer factory methods (e.g. Goal.create(), CalendarEvent.create()) throw
 * IllegalArgumentException for business-rule validation failures (ADR-020). Without
 * this mapper those exceptions fell through to an unmapped 500 instead of a 400.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    private static final String CODE = "BAD_REQUEST";

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        ErrorResponse errorResponse = new ErrorResponse(400, CODE, exception.getMessage());

        AppLogger.warn("Validation error: [%s] %s", CODE, exception.getMessage());

        return Response
            .status(400)
            .entity(errorResponse)
            .build();
    }
}
