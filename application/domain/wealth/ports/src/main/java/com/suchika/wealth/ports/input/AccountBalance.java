package com.suchika.wealth.ports.input;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Return type for {@link AccountUseCase#getAccountBalance}.
 * Computed balance = opening_balance + SUM(CREDIT) - SUM(DEBIT) for the account's
 * full transaction ledger (Epic 8 Phase 1, Bug 2 fix — opening_balance alone is a
 * snapshot at account creation, not the current balance). balanceAsOf is the date
 * the account's opening_balance was last known accurate (manually set) — null when
 * never recorded. It does not advance automatically as transactions are added; it
 * only describes the opening_balance snapshot itself.
 */
public record AccountBalance(UUID accountId, BigDecimal openingBalance, BigDecimal totalCredits,
                              BigDecimal totalDebits, BigDecimal currentBalance, LocalDate balanceAsOf) {
}
