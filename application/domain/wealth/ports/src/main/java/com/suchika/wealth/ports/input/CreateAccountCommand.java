package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.AccountType;

import java.math.BigDecimal;

public record CreateAccountCommand(
        String accountName,
        AccountType accountType,
        String institutionName,
        String currency,
        BigDecimal openingBalance,
        BigDecimal creditLimit,
        BigDecimal interestRate,
        BigDecimal emiAmount
) {}
