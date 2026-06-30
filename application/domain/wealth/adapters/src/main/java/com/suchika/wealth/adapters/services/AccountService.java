package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.AccountBalance;
import com.suchika.wealth.ports.input.AccountUseCase;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountService implements AccountUseCase {

    private static final String ACCOUNT_NOT_FOUND = "Account not found: ";

    private final AccountRepository repository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository repository, TransactionRepository transactionRepository) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Account createAccount(UUID profileId, CreateAccountCommand command) {
        if (command.accountName() == null || command.accountName().isBlank()) {
            throw new BadRequestException("account_name is required");
        }
        if (command.accountType() == null) {
            throw new BadRequestException("account_type is required");
        }
        if (command.institutionName() == null || command.institutionName().isBlank()) {
            throw new BadRequestException("institution_name is required");
        }

        Account account = Account.builder()
                .profileId(profileId)
                .accountName(command.accountName())
                .accountType(command.accountType())
                .institutionName(command.institutionName())
                .openingBalance(command.openingBalance() != null ? command.openingBalance() : BigDecimal.ZERO)
                .creditLimit(command.creditLimit())
                .interestRate(command.interestRate())
                .emiAmount(command.emiAmount())
                .build();

        Account saved = repository.save(account);
        AppLogger.info("Account created: %s (%s)", saved.getId(), command.accountType());
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

    @Override
    public AccountBalance getAccountBalance(UUID accountId, UUID profileId) {
        Account account = repository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND + accountId));

        BigDecimal opening = account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal totalCredits = transactionRepository.sumAmountByTxnType(accountId, profileId, TxnType.CREDIT);
        BigDecimal totalDebits = transactionRepository.sumAmountByTxnType(accountId, profileId, TxnType.DEBIT);
        BigDecimal current = opening.add(totalCredits).subtract(totalDebits);

        return new AccountBalance(accountId, opening, totalCredits, totalDebits, current);
    }

    @Override
    @Transactional
    public Account updateAccountClassification(UUID id, String category, String liquidityTier, String purposeTag) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_NOT_FOUND + id));

        java.util.Map<String, String> metadata = new java.util.HashMap<>(account.getMetadata());
        if (category != null) metadata.put("category", category);
        if (liquidityTier != null) metadata.put("liquidity_tier", liquidityTier);
        if (purposeTag != null) metadata.put("purpose_tag", purposeTag);
        account.setMetadata(metadata);

        Account saved = repository.save(account);
        AppLogger.info("Account classification updated: %s", id);
        return saved;
    }
}
