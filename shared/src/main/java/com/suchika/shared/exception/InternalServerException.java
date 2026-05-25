package com.suchika.shared.exception;

/**
 * 500 Internal Server Error - Unexpected server error.
 */
public class InternalServerException extends ApplicationException {

    public InternalServerException(String message) {
        super(500, "INTERNAL_SERVER_ERROR", message);
    }

    public InternalServerException(String message, String details) {
        super(500, "INTERNAL_SERVER_ERROR", message, details);
    }

    public InternalServerException(String message, Throwable cause) {
        super(500, "INTERNAL_SERVER_ERROR", message, cause);
    }
}
