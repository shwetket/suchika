package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.UploadErrorLog;

import java.time.Instant;
import java.util.List;

public class UploadErrorLogResponse {

    @JsonProperty("error_type")
    public String errorType;

    @JsonProperty("missing_columns")
    public List<String> missingColumns;

    @JsonProperty("error_detail")
    public String errorDetail;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static UploadErrorLogResponse from(UploadErrorLog log) {
        UploadErrorLogResponse r = new UploadErrorLogResponse();
        r.errorType = log.getErrorType();
        r.missingColumns = log.getMissingColumns();
        r.errorDetail = log.getErrorDetail();
        r.createdAt = log.getCreatedAt();
        return r;
    }
}
