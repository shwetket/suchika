package com.suchika.shared.exception;

/**
 * Base application exception for all domain exceptions.
 * Includes HTTP status code and error code for OpenAPI responses.
 */
public class ApplicationException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final String details;

    public ApplicationException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApplicationException(int statusCode, String errorCode, String message, String details) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApplicationException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.details = null;
    }

    public ApplicationException(int statusCode, String errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.details = details;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}
