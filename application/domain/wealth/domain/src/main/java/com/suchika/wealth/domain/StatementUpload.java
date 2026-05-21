package com.suchika.wealth.domain;

import java.time.Instant;
import java.util.UUID;

public class StatementUpload {

    private UUID id;
    private UUID accountId;
    private String fileName;
    private Instant uploadDate;
    private UploadStatus status;

    public StatementUpload() {}

    private StatementUpload(Builder builder) {
        this.id = builder.id;
        this.accountId = builder.accountId;
        this.fileName = builder.fileName;
        this.uploadDate = builder.uploadDate;
        this.status = builder.status;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID accountId;
        private String fileName;
        private Instant uploadDate;
        private UploadStatus status = UploadStatus.PENDING;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder accountId(UUID accountId) { this.accountId = accountId; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder uploadDate(Instant uploadDate) { this.uploadDate = uploadDate; return this; }
        public Builder status(UploadStatus status) { this.status = status; return this; }
        public StatementUpload build() { return new StatementUpload(this); }
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getFileName() { return fileName; }
    public Instant getUploadDate() { return uploadDate; }
    public UploadStatus getStatus() { return status; }

    public void setStatus(UploadStatus status) { this.status = status; }
}
