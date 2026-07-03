package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListTransactionsResponse {

    @JsonProperty("transactions")
    public List<TransactionResponse> transactions;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListTransactionsResponse(List<TransactionResponse> transactions) {
        this.transactions = transactions;
        this.totalSize = transactions.size();
    }

    /**
     * Paginated variant (v0.6 UX item) — totalSize is the grand total matching
     * the filter across all pages, not just transactions.size().
     */
    public ListTransactionsResponse(List<TransactionResponse> transactions, long totalSize, int page, int size) {
        this.transactions = transactions;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
