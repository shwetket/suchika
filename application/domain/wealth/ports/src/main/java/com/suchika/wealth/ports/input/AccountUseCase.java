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
}
