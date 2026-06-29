package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.ports.input.UploadResult;

import java.time.Instant;
import java.util.List;
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

    @JsonProperty("inserted_count")
    public int insertedCount;

    @JsonProperty("skipped_duplicates")
    public List<SkippedTransactionDto> skippedDuplicates;

    public static StatementUploadResponse from(StatementUpload upload) {
        StatementUploadResponse r = new StatementUploadResponse();
        r.uploadId = upload.getId();
        r.accountId = upload.getAccountId();
        r.fileName = upload.getFileName();
        r.uploadDate = upload.getUploadDate();
        r.status = upload.getStatus() != null ? upload.getStatus().name() : null;
        r.insertedCount = 0;
        r.skippedDuplicates = List.of();
        return r;
    }

    public static StatementUploadResponse from(UploadResult result) {
        StatementUpload upload = result.getUpload();
        StatementUploadResponse r = new StatementUploadResponse();
        r.uploadId = upload.getId();
        r.accountId = upload.getAccountId();
        r.fileName = upload.getFileName();
        r.uploadDate = upload.getUploadDate();
        r.status = upload.getStatus() != null ? upload.getStatus().name() : null;
        r.insertedCount = result.getInsertedCount();
        r.skippedDuplicates = result.getSkippedDuplicates().stream()
                .map(s -> new SkippedTransactionDto(s.txnDate(), s.amount(), s.description()))
                .toList();
        return r;
    }
}
