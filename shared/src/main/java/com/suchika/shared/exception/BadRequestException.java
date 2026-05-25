package com.suchika.shared.exception;

/**
 * 400 Bad Request - Invalid request parameters or malformed data.
 */
public class BadRequestException extends ApplicationException {

    public BadRequestException(String message) {
        super(400, "BAD_REQUEST", message);
    }

    public BadRequestException(String message, String details) {
        super(400, "BAD_REQUEST", message, details);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, "BAD_REQUEST", message, cause);
    }
}
