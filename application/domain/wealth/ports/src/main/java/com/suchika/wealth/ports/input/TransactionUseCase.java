package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.ExpenseCategory;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {

    /**
     * Lists transactions for an account, optionally scoped to profileId (ADR-006).
     * When profileId is non-null, the adapter filters out transactions whose
     * account does not belong to that profile — closes the v0.5 Phase 0 data
     * isolation gap where this call site previously hardcoded profileId to null.
     */
    List<Transaction> listByAccount(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType);

    /**
     * Creates a single manually-entered transaction (Q7). The transaction is saved
     * with metadata.source = "MANUAL" so it stays distinguishable from CSV-imported
     * rows for future analysis. No dedup check — manual entries are always intentional.
     */
    Transaction create(UUID accountId, UUID profileId, CreateTransactionCommand command);

    Transaction getById(UUID id);

    /**
     * Sets the Epic 8 Phase 2 expense category on a single transaction (Use Case 8.3,
     * manual tagging — Q24). Merges into the existing metadata map rather than
     * replacing it, mirroring the account classification write pattern.
     */
    Transaction updateCategory(UUID id, ExpenseCategory category);

    /**
     * Tags a caller-selected list of transactions with the same category in one call
     * (Q24 resolution — bulk-by-selection, not a rules engine). Each transaction is
     * looked up and updated individually; not atomic across the whole list beyond the
     * surrounding @Transactional boundary in the implementation.
     */
    List<Transaction> bulkUpdateCategory(List<UUID> ids, ExpenseCategory category);
}
