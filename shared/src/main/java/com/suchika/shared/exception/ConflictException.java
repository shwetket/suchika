package com.suchika.shared.exception;

/**
 * 409 Conflict - Request conflicts with current state (e.g., duplicate resource).
 */
public class ConflictException extends ApplicationException {

    private static final String CODE = "CONFLICT";

    public ConflictException(String message) {
        super(409, CODE, message);
    }

    public ConflictException(String message, String details) {
        super(409, CODE, message, details);
    }

    public ConflictException(String message, Throwable cause) {
        super(409, CODE, message, cause);
    }
}
