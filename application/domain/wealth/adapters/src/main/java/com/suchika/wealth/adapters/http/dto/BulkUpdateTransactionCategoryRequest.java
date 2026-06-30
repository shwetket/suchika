package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;
import java.util.UUID;

/**
 * Bulk-tag-by-selection request (Q24) — tags a caller-selected list of transaction
 * IDs with one category in a single call. Not a rules engine: the client decides
 * which transactions to select.
 */
@RegisterForReflection
public class BulkUpdateTransactionCategoryRequest {

    @JsonProperty("transaction_ids")
    public List<UUID> transactionIds;

    @JsonProperty("category")
    public String category;
}
