package com.suchika.wealth.adapters.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transaction", schema = "wealth")
public class TransactionEntity extends PanacheEntityBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {};

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
    public UUID accountId;

    @Column(name = "upload_id", nullable = false, columnDefinition = "uuid")
    public UUID uploadId;

    @Column(name = "txn_date", nullable = false)
    public LocalDate txnDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    public BigDecimal amount;

    @Column(name = "txn_type", nullable = false, length = 10)
    public String txnType;

    @Column(name = "description", nullable = false)
    public String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static TransactionEntity from(Transaction txn) {
        TransactionEntity e = new TransactionEntity();
        e.id = txn.getId();
        e.accountId = txn.getAccountId();
        e.uploadId = txn.getUploadId();
        e.txnDate = txn.getTxnDate();
        e.amount = txn.getAmount();
        e.txnType = txn.getTxnType() != null ? txn.getTxnType().name() : null;
        e.description = txn.getDescription();
        e.createdAt = txn.getCreatedAt();
        e.metadata = writeMetadata(txn.getMetadata());
        return e;
    }

    public Transaction toDomain() {
        return Transaction.builder()
                .id(id)
                .accountId(accountId)
                .uploadId(uploadId)
                .txnDate(txnDate)
                .amount(amount)
                .txnType(TxnType.valueOf(txnType))
                .description(description)
                .createdAt(createdAt)
                .metadata(readMetadata(metadata))
                .build();
    }

    private static String writeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            AppLogger.warn("Failed to serialize transaction metadata, defaulting to empty object: %s", e.getMessage());
            return "{}";
        }
    }

    private static Map<String, String> readMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(json, METADATA_TYPE);
        } catch (java.io.IOException e) {
            AppLogger.warn("Failed to deserialize transaction metadata, defaulting to empty map: %s", e.getMessage());
            return new HashMap<>();
        }
    }
}
