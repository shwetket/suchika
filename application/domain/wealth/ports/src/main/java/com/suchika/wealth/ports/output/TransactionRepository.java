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

    List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType);

    /**
     * Paginated variant of {@link #findByAccountId} — v0.6 UX item (transaction list
     * pagination). {@code page} is 0-indexed. Same filter predicate as the
     * unpaginated method; use {@link #countByAccountId} for the total matching count.
     */
    List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType,
                                       int page, int size);

    /**
     * Total count of transactions matching the same filter predicate as
     * {@link #findByAccountId}, ignoring pagination — used to compute total pages.
     */
    long countByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType);

    boolean existsByDeduplicationKey(UUID accountId, UUID profileId, LocalDate txnDate, BigDecimal amount, TxnType txnType);

    /**
     * Sums the absolute amount of all transactions of the given type for an account,
     * optionally scoped to profileId (ADR-006). Used by the balance calculation
     * (opening_balance + SUM(CREDIT) - SUM(DEBIT)) — Epic 8 Phase 1, Bug 2 fix.
     * Returns BigDecimal.ZERO when there are no matching rows.
     */
    BigDecimal sumAmountByTxnType(UUID accountId, UUID profileId, TxnType txnType);
}
