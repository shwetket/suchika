package com.suchika.shared.exception;

/**
 * 501 Not Implemented - Server does not support the functionality required.
 */
public class NotImplementedException extends ApplicationException {

    private static final String CODE = "NOT_IMPLEMENTED";

    public NotImplementedException(String message) {
        super(501, CODE, message);
    }

    public NotImplementedException(String message, String details) {
        super(501, CODE, message, details);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(501, CODE, message, cause);
    }
}
