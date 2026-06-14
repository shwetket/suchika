package com.suchika.shared.exception;

/**
 * 404 Not Found - Requested resource does not exist.
 */
public class NotFoundException extends ApplicationException {

    private static final String CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(404, CODE, message);
    }

    public NotFoundException(String message, String details) {
        super(404, CODE, message, details);
    }

    public NotFoundException(String message, Throwable cause) {
        super(404, CODE, message, cause);
    }
}
