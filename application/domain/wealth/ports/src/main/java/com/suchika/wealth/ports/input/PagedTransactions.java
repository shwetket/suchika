package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.Transaction;

import java.util.List;

/**
 * Result of a paginated transaction list query — v0.6 UX item (transaction list
 * pagination). {@code totalCount} is the count across all pages matching the
 * same filter, not just {@code transactions.size()}.
 */
public record PagedTransactions(List<Transaction> transactions, long totalCount) {
}
