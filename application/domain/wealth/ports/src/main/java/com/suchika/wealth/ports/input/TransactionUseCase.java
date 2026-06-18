package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {

    List<Transaction> listByAccount(UUID accountId, LocalDate from, LocalDate to, TxnType txnType);

    Transaction getById(UUID id);
}
