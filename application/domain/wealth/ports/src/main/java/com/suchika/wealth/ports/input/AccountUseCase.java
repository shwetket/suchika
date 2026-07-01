package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.domain.AmortizationSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountUseCase {

    Account createAccount(UUID profileId, CreateAccountCommand command);

    Account getAccount(UUID id);

    List<Account> listAccounts(UUID profileId, AccountType accountType, Boolean isActive);

    Account updateAccount(UUID id, String accountName, BigDecimal openingBalance, BigDecimal creditLimit,
                           BigDecimal interestRate, BigDecimal emiAmount, Boolean isActive);

    void deactivateAccount(UUID id);

    /**
     * Computes the current balance for an account: opening_balance + SUM(CREDIT) - SUM(DEBIT)
     * over its full transaction ledger. Epic 8 Phase 1, Bug 2 fix — opening_balance alone is
     * only the snapshot recorded at account creation, not the running balance.
     */
    AccountBalance getAccountBalance(UUID accountId, UUID profileId);

    /**
     * Sets Epic 8 classification metadata (category, liquidity_tier, purpose_tag,
     * joint_owners, and loan fields) on an account. Thin wrapper that merges the given
     * keys into the existing metadata map — mirrors the transaction.metadata write pattern;
     * does not replace the whole map.
     *
     * <p>category is reserved but NOT consumed by any computation until Phase 2 — storing
     * it now is intentional so no second migration/contract change is needed later.
     *
     * <p>jointOwners (ADR-016 Decision 2, Phase 2) is a list of co-owner profile_id
     * strings, stored comma-joined under metadata.joint_owners — attribution/display
     * only, never a query predicate. profile_id remains the single column every query
     * filters on (ADR-006 unchanged).
     *
     * <p>Loan fields (loanOriginalPrincipal, loanStartDate, loanTenureMonths,
     * linkedOffsetAccountId) are Epic 8 Phase 3 prerequisites for amortization calculation.
     */
    Account updateAccountClassification(UUID id, String category, String liquidityTier, String purposeTag,
                                         List<String> jointOwners, String loanOriginalPrincipal,
                                         String loanStartDate, String loanTenureMonths,
                                         String linkedOffsetAccountId);

    /**
     * Computes the amortization schedule summary for a loan account.
     * Requires interest_rate on the account and original_principal, loan_start_date,
     * original_tenure_months in account.metadata (set via PATCH /classification).
     * Epic 8 Phase 3.
     */
    AmortizationSummary getAmortization(UUID accountId, UUID profileId);
}
