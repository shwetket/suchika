package com.suchika.shared.exception;

/**
 * 501 Not Implemented - Server does not support the functionality required.
 */
public class NotImplementedException extends ApplicationException {

    public NotImplementedException(String message) {
        super(501, "NOT_IMPLEMENTED", message);
    }

    public NotImplementedException(String message, String details) {
        super(501, "NOT_IMPLEMENTED", message, details);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(501, "NOT_IMPLEMENTED", message, cause);
    }
}
