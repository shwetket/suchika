package com.suchika.shared.exception;

/**
 * 406 Not Acceptable - Server cannot produce response matching client's Accept header.
 */
public class NotAcceptableException extends ApplicationException {

    private static final String CODE = "NOT_ACCEPTABLE";

    public NotAcceptableException(String message) {
        super(406, CODE, message);
    }

    public NotAcceptableException(String message, String details) {
        super(406, CODE, message, details);
    }

    public NotAcceptableException(String message, Throwable cause) {
        super(406, CODE, message, cause);
    }
}
