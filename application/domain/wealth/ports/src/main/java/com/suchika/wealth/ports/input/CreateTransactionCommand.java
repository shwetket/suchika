package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.TxnType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionCommand(
        LocalDate txnDate,
        BigDecimal amount,
        TxnType txnType,
        String description
) {}
