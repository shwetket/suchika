package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ListTransactionsResponse {

    @JsonProperty("transactions")
    public List<TransactionResponse> transactions;

    public ListTransactionsResponse(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }
}
