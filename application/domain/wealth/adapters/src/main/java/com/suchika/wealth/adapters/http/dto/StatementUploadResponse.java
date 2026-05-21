package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.StatementUpload;

import java.time.Instant;
import java.util.UUID;

public class StatementUploadResponse {

    @JsonProperty("upload_id")
    public UUID uploadId;

    @JsonProperty("account_id")
    public UUID accountId;

    @JsonProperty("file_name")
    public String fileName;

    @JsonProperty("upload_date")
    public Instant uploadDate;

    @JsonProperty("status")
    public String status;

    public static StatementUploadResponse from(StatementUpload upload) {
        StatementUploadResponse r = new StatementUploadResponse();
        r.uploadId = upload.getId();
        r.accountId = upload.getAccountId();
        r.fileName = upload.getFileName();
        r.uploadDate = upload.getUploadDate();
        r.status = upload.getStatus() != null ? upload.getStatus().name() : null;
        return r;
    }
}
