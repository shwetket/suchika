package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class TransactionResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("account_id")
    public UUID accountId;

    @JsonProperty("upload_id")
    public UUID uploadId;

    @JsonProperty("txn_date")
    public LocalDate txnDate;

    @JsonProperty("amount")
    public BigDecimal amount;

    @JsonProperty("txn_type")
    public String txnType;

    @JsonProperty("description")
    public String description;

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("metadata")
    public Map<String, String> metadata;

    public static TransactionResponse from(Transaction txn) {
        TransactionResponse r = new TransactionResponse();
        r.id = txn.getId();
        r.accountId = txn.getAccountId();
        r.uploadId = txn.getUploadId();
        r.txnDate = txn.getTxnDate();
        r.amount = txn.getAmount();
        r.txnType = txn.getTxnType() != null ? txn.getTxnType().name() : null;
        r.description = txn.getDescription();
        r.createdAt = txn.getCreatedAt();
        r.metadata = txn.getMetadata();
        return r;
    }
}
