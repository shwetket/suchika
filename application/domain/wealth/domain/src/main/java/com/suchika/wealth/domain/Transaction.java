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

    /**
     * Factory for newly-created transactions. Validates {@code amount} since the
     * DB-level {@code CHECK (amount >= 0)} constraint was removed in the Flyway
     * consolidation (business-rule CHECKs moved to the application layer per
     * documents/OpenQuestions.md Q31/Q45). Direction is carried by {@code txnType},
     * never by the sign of amount — amounts are always stored non-negative.
     */
    public static Transaction create(UUID accountId, UUID uploadId, LocalDate txnDate, BigDecimal amount,
                                      TxnType txnType, String description, Map<String, String> metadata) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be null and must be >= 0");
        }
        return new Builder()
                .accountId(accountId)
                .uploadId(uploadId)
                .txnDate(txnDate)
                .amount(amount)
                .txnType(txnType)
                .description(description)
                .metadata(metadata)
                .build();
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
