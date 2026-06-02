package com.suchika.shared.exception;

/**
 * 401 Unauthorized - Authentication failed or missing credentials.
 */
public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(String message) {
        super(401, "UNAUTHORIZED", message);
    }

    public UnauthorizedException(String message, String details) {
        super(401, "UNAUTHORIZED", message, details);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, "UNAUTHORIZED", message, cause);
    }
}
