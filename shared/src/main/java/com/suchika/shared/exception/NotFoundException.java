package com.suchika.shared.exception;

/**
 * 404 Not Found - Requested resource does not exist.
 */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String message) {
        super(404, "NOT_FOUND", message);
    }

    public NotFoundException(String message, String details) {
        super(404, "NOT_FOUND", message, details);
    }

    public NotFoundException(String message, Throwable cause) {
        super(404, "NOT_FOUND", message, cause);
    }
}
