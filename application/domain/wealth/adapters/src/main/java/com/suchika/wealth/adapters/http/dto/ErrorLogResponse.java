package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.ErrorLog;

import java.time.Instant;

public class ErrorLogResponse {

    @JsonProperty("error_code")
    public String errorCode;

    @JsonProperty("http_status")
    public int httpStatus;

    public String message;

    public String details;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static ErrorLogResponse from(ErrorLog log) {
        ErrorLogResponse r = new ErrorLogResponse();
        r.errorCode = log.getErrorCode();
        r.httpStatus = log.getHttpStatus();
        r.message = log.getMessage();
        r.details = log.getDetails();
        r.createdAt = log.getCreatedAt();
        return r;
    }
}
