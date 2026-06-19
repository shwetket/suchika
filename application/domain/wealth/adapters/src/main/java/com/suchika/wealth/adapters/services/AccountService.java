package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.input.AccountUseCase;
import com.suchika.wealth.ports.output.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountService implements AccountUseCase {

    private static final String ACCOUNT_NOT_FOUND = "Account not found: ";

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Account createAccount(UUID profileId, String accountName, AccountType accountType, String institutionName,
                                  BigDecimal openingBalance, BigDecimal creditLimit,
                                  BigDecimal interestRate, BigDecimal emiAmount) {
        if (accountName == null || accountName.isBlank()) {
            throw new BadRequestException("account_name is required");
        }
        if (accountType == null) {
            throw new BadRequestException("account_type is required");
        }
        if (institutionName == null || institutionName.isBlank()) {
            throw new BadRequestException("institution_name is required");
        }

        Account account = Account.builder()
                .profileId(profileId)
                .accountName(accountName)
                .accountType(accountType)
                .institutionName(institutionName)
                .openingBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO)
                .creditLimit(creditLimit)
                .interestRate(interestRate)
                .emiAmount(emiAmount)
                .build();

        Account saved = repository.save(account);
        AppLogger.info("Account created: %s (%s)", saved.getId(), accountType);
        return saved;
    }

    @Override
    public Account getAccount(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND + id));
    }

    @Override
    public List<Account> listAccounts(UUID profileId, AccountType accountType, Boolean isActive) {
        return repository.findAll(profileId, accountType, isActive);
    }

    @Override
    @Transactional
    public Account updateAccount(UUID id, String accountName, BigDecimal openingBalance, BigDecimal creditLimit,
                                  BigDecimal interestRate, BigDecimal emiAmount, Boolean isActive) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND + id));

        if (accountName != null) {
            if (accountName.isBlank()) throw new BadRequestException("account_name must not be blank");
            account.setAccountName(accountName);
        }
        if (openingBalance != null) account.setOpeningBalance(openingBalance);
        if (creditLimit != null) account.setCreditLimit(creditLimit);
        if (interestRate != null) account.setInterestRate(interestRate);
        if (emiAmount != null) account.setEmiAmount(emiAmount);

        if (Boolean.FALSE.equals(isActive)) {
            requireNoTransactions(id);
        }
        if (isActive != null) account.setActive(isActive);

        return repository.save(account);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND + id));

        requireNoTransactions(id);

        account.setActive(false);
        repository.save(account);
        AppLogger.info("Account deactivated: %s", id);
    }

    private void requireNoTransactions(UUID accountId) {
        if (repository.hasTransactions(accountId)) {
            throw new ConflictException("Cannot deactivate account with existing transactions");
        }
    }
}
