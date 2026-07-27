package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public class CreateTransactionRequest {

    @JsonProperty("txn_date")
    public String txnDate;

    @JsonProperty("amount")
    public BigDecimal amount;

    @JsonProperty("txn_type")
    public String txnType;

    @JsonProperty("description")
    public String description;
}
