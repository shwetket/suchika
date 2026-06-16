package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    List<Account> findAll(AccountType accountType, Boolean isActive);

    boolean existsById(UUID id);

    boolean hasTransactions(UUID accountId);
}
