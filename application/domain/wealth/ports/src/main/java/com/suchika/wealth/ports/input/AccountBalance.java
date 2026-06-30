package com.suchika.wealth.ports.input;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Return type for {@link AccountUseCase#getAccountBalance}.
 * Computed balance = opening_balance + SUM(CREDIT) - SUM(DEBIT) for the account's
 * full transaction ledger (Epic 8 Phase 1, Bug 2 fix — opening_balance alone is a
 * snapshot at account creation, not the current balance).
 */
public record AccountBalance(UUID accountId, BigDecimal openingBalance, BigDecimal totalCredits,
                              BigDecimal totalDebits, BigDecimal currentBalance) {
}
