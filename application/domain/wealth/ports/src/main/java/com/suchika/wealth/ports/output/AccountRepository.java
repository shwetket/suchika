package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    /**
     * Profile-scoped lookup — v0.5.1 remediation (Tier B). Returns empty both when
     * the id doesn't exist at all AND when it exists but belongs to a different
     * profile, so callers can 404 without leaking cross-profile existence.
     */
    Optional<Account> findById(UUID id, UUID profileId);

    List<Account> findAll(UUID profileId, AccountType accountType, Boolean isActive);

    boolean existsById(UUID id);

    boolean hasTransactions(UUID accountId);
}
