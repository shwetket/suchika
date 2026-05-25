package com.suchika.shared.exception;

/**
 * 406 Not Acceptable - Server cannot produce response matching client's Accept header.
 */
public class NotAcceptableException extends ApplicationException {

    public NotAcceptableException(String message) {
        super(406, "NOT_ACCEPTABLE", message);
    }

    public NotAcceptableException(String message, String details) {
        super(406, "NOT_ACCEPTABLE", message, details);
    }

    public NotAcceptableException(String message, Throwable cause) {
        super(406, "NOT_ACCEPTABLE", message, cause);
    }
}
