package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SkippedTransactionDto {

    @JsonProperty("txn_date")
    public LocalDate txnDate;

    @JsonProperty("amount")
    public BigDecimal amount;

    @JsonProperty("description")
    public String description;

    public SkippedTransactionDto(LocalDate txnDate, BigDecimal amount, String description) {
        this.txnDate = txnDate;
        this.amount = amount;
        this.description = description;
    }
}
