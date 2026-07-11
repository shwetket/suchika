package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.ports.input.AccountBalance;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
public class AccountBalanceResponse {

    @JsonProperty("account_id")
    public UUID accountId;

    @JsonProperty("opening_balance")
    public BigDecimal openingBalance;

    @JsonProperty("total_credits")
    public BigDecimal totalCredits;

    @JsonProperty("total_debits")
    public BigDecimal totalDebits;

    @JsonProperty("current_balance")
    public BigDecimal currentBalance;

    @JsonProperty("balance_as_of")
    public LocalDate balanceAsOf;

    public static AccountBalanceResponse from(AccountBalance balance) {
        AccountBalanceResponse r = new AccountBalanceResponse();
        r.accountId = balance.accountId();
        r.openingBalance = balance.openingBalance();
        r.totalCredits = balance.totalCredits();
        r.totalDebits = balance.totalDebits();
        r.currentBalance = balance.currentBalance();
        r.balanceAsOf = balance.balanceAsOf();
        return r;
    }
}
