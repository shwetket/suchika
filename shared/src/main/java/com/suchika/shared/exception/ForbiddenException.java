package com.suchika.shared.exception;

/**
 * 403 Forbidden - User authenticated but lacks permissions.
 */
public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message);
    }

    public ForbiddenException(String message, String details) {
        super(403, "FORBIDDEN", message, details);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(403, "FORBIDDEN", message, cause);
    }
}
