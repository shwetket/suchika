package com.suchika.shared.exception;

/**
 * 400 Bad Request - Invalid request parameters or malformed data.
 */
public class BadRequestException extends ApplicationException {

    private static final String CODE = "BAD_REQUEST";

    public BadRequestException(String message) {
        super(400, CODE, message);
    }

    public BadRequestException(String message, String details) {
        super(400, CODE, message, details);
    }

    public BadRequestException(String message, Throwable cause) {
        super(400, CODE, message, cause);
    }
}
