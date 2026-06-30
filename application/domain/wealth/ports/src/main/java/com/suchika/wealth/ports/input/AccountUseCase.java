package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;

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
     * Sets Epic 8 classification metadata (category, liquidity_tier, purpose_tag) on an
     * account. Thin wrapper that merges the given keys into the existing metadata map —
     * mirrors the transaction.metadata write pattern; does not replace the whole map.
     * Only the three reserved keys this phase cares about are accepted here; joint_owners
     * is out of Phase 1 scope (ADR-016, Phase 2).
     *
     * <p>category is reserved but NOT consumed by any computation until Phase 2 — storing
     * it now is intentional so no second migration/contract change is needed later.
     */
    Account updateAccountClassification(UUID id, String category, String liquidityTier, String purposeTag);
}
