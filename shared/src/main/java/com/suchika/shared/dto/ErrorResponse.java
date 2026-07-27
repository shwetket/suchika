package com.suchika.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

/**
 * Standard error response DTO for all API endpoints.
 * Includes status code, error message, and optional details.
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String errorCode;
    private String message;
    private String details;
    private long timestamp;

    public ErrorResponse() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ErrorResponse(int status, String errorCode, String message) {
        this();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorResponse(int status, String errorCode, String message, String details) {
        this();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
