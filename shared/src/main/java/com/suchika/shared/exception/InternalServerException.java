package com.suchika.shared.exception;

/**
 * 500 Internal Server Error - Unexpected server error.
 */
public class InternalServerException extends ApplicationException {

    private static final String CODE = "INTERNAL_SERVER_ERROR";

    public InternalServerException(String message) {
        super(500, CODE, message);
    }

    public InternalServerException(String message, String details) {
        super(500, CODE, message, details);
    }

    public InternalServerException(String message, Throwable cause) {
        super(500, CODE, message, cause);
    }
}
