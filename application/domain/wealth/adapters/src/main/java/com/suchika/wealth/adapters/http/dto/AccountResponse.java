package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.Account;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    @JsonProperty("account_id")
    public UUID accountId;

    @JsonProperty("account_name")
    public String accountName;

    @JsonProperty("account_type")
    public String accountType;

    @JsonProperty("institution_name")
    public String institutionName;

    @JsonProperty("opening_balance")
    public BigDecimal openingBalance;

    @JsonProperty("credit_limit")
    public BigDecimal creditLimit;

    @JsonProperty("interest_rate")
    public BigDecimal interestRate;

    @JsonProperty("emi_amount")
    public BigDecimal emiAmount;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static AccountResponse from(Account account) {
        AccountResponse r = new AccountResponse();
        r.accountId = account.getId();
        r.accountName = account.getAccountName();
        r.accountType = account.getAccountType() != null ? account.getAccountType().name() : null;
        r.institutionName = account.getInstitutionName();
        r.openingBalance = account.getOpeningBalance();
        r.creditLimit = account.getCreditLimit();
        r.interestRate = account.getInterestRate();
        r.emiAmount = account.getEmiAmount();
        r.active = account.isActive();
        r.createdAt = account.getCreatedAt();
        return r;
    }
}
