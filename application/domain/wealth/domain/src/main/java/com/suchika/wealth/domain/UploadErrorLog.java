package com.suchika.wealth.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UploadErrorLog {

    private final UUID id;
    private final UUID uploadId;
    private final String errorType;
    private final List<String> missingColumns;
    private final String errorDetail;
    private final Instant createdAt;

    private UploadErrorLog(Builder builder) {
        this.id = builder.id;
        this.uploadId = builder.uploadId;
        this.errorType = builder.errorType;
        this.missingColumns = builder.missingColumns;
        this.errorDetail = builder.errorDetail;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID uploadId;
        private String errorType;
        private List<String> missingColumns;
        private String errorDetail;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder uploadId(UUID uploadId) { this.uploadId = uploadId; return this; }
        public Builder errorType(String errorType) { this.errorType = errorType; return this; }
        public Builder missingColumns(List<String> missingColumns) { this.missingColumns = missingColumns; return this; }
        public Builder errorDetail(String errorDetail) { this.errorDetail = errorDetail; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public UploadErrorLog build() { return new UploadErrorLog(this); }
    }

    public UUID getId() { return id; }
    public UUID getUploadId() { return uploadId; }
    public String getErrorType() { return errorType; }
    public List<String> getMissingColumns() { return missingColumns; }
    public String getErrorDetail() { return errorDetail; }
    public Instant getCreatedAt() { return createdAt; }
}
