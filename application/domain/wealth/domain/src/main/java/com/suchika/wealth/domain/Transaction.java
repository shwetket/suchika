package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class Transaction {

    private UUID id;
    private UUID accountId;
    private UUID uploadId;
    private LocalDate txnDate;
    private BigDecimal amount;
    private TxnType txnType;
    private String description;
    private Map<String, String> metadata;
    private Instant createdAt;

    public Transaction() {}

    private Transaction(Builder builder) {
        this.id = builder.id;
        this.accountId = builder.accountId;
        this.uploadId = builder.uploadId;
        this.txnDate = builder.txnDate;
        this.amount = builder.amount;
        this.txnType = builder.txnType;
        this.description = builder.description;
        this.metadata = builder.metadata;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID accountId;
        private UUID uploadId;
        private LocalDate txnDate;
        private BigDecimal amount;
        private TxnType txnType;
        private String description;
        private Map<String, String> metadata;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder accountId(UUID accountId) { this.accountId = accountId; return this; }
        public Builder uploadId(UUID uploadId) { this.uploadId = uploadId; return this; }
        public Builder txnDate(LocalDate txnDate) { this.txnDate = txnDate; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder txnType(TxnType txnType) { this.txnType = txnType; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Transaction build() { return new Transaction(this); }
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public UUID getUploadId() { return uploadId; }
    public LocalDate getTxnDate() { return txnDate; }
    public BigDecimal getAmount() { return amount; }
    public TxnType getTxnType() { return txnType; }
    public String getDescription() { return description; }
    public Map<String, String> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
