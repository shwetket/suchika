package com.suchika.shared.exception;

/**
 * 401 Unauthorized - Authentication failed or missing credentials.
 */
public class UnauthorizedException extends ApplicationException {

    private static final String CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(401, CODE, message);
    }

    public UnauthorizedException(String message, String details) {
        super(401, CODE, message, details);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(401, CODE, message, cause);
    }
}
