package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    List<Transaction> findByAccountId(UUID accountId, LocalDate from, LocalDate to, TxnType txnType);

    boolean existsByUniqueKey(UUID accountId, LocalDate txnDate, BigDecimal amount, TxnType txnType, String description);
}
