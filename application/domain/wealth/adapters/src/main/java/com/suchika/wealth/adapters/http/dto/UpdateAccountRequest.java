package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public class UpdateAccountRequest {

    @JsonProperty("account_name")
    public String accountName;

    @JsonProperty("opening_balance")
    public BigDecimal openingBalance;

    @JsonProperty("credit_limit")
    public BigDecimal creditLimit;

    @JsonProperty("interest_rate")
    public BigDecimal interestRate;

    @JsonProperty("emi_amount")
    public BigDecimal emiAmount;

    @JsonProperty("is_active")
    public Boolean active;
}
