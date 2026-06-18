package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.*;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransactionPanacheRepository.
 * Requires a running local PostgreSQL (app_db).
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestTransaction
class TransactionPanacheRepositoryTest {

    @Inject
    TransactionRepository repository;

    @Inject
    AccountRepository accountRepository;

    @Inject
    StatementUploadRepository uploadRepository;

    @Test
    void save_andFindById_roundTrip() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        Transaction saved = repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 6, 1),
                new BigDecimal("1000.00"), TxnType.CREDIT, "Salary"));

        assertNotNull(saved.getId());
        assertEquals(accountId, saved.getAccountId());
        assertEquals(uploadId, saved.getUploadId());
        assertEquals(LocalDate.of(2026, 6, 1), saved.getTxnDate());
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getAmount()));
        assertEquals(TxnType.CREDIT, saved.getTxnType());
        assertEquals("Salary", saved.getDescription());

        Optional<Transaction> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Salary", found.get().getDescription());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByAccountId_noFilters_returnsAll() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 3, 1),
                new BigDecimal("500.00"), TxnType.DEBIT, "Rent"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 4, 1),
                new BigDecimal("200.00"), TxnType.DEBIT, "Electricity"));

        List<Transaction> result = repository.findByAccountId(accountId, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void findByAccountId_fromFilter_excludesOlderRows() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 1, 1),
                new BigDecimal("100.00"), TxnType.DEBIT, "January bill"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 6, 1),
                new BigDecimal("200.00"), TxnType.DEBIT, "June bill"));

        List<Transaction> result = repository.findByAccountId(accountId, LocalDate.of(2026, 6, 1), null, null);

        assertEquals(1, result.size());
        assertEquals("June bill", result.get(0).getDescription());
    }

    @Test
    void findByAccountId_toFilter_excludesNewerRows() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 1, 15),
                new BigDecimal("150.00"), TxnType.DEBIT, "January purchase"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 2, 10),
                new BigDecimal("300.00"), TxnType.DEBIT, "February purchase"));

        List<Transaction> result = repository.findByAccountId(accountId, null, LocalDate.of(2026, 1, 31), null);

        assertEquals(1, result.size());
        assertEquals("January purchase", result.get(0).getDescription());
    }

    @Test
    void findByAccountId_txnTypeFilter_returnsOnlyMatching() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 5, 1),
                new BigDecimal("5000.00"), TxnType.CREDIT, "Salary credit"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, 5, 5),
                new BigDecimal("200.00"), TxnType.DEBIT, "ATM withdrawal"));

        List<Transaction> result = repository.findByAccountId(accountId, null, null, TxnType.CREDIT);

        assertEquals(1, result.size());
        assertEquals(TxnType.CREDIT, result.get(0).getTxnType());
        assertEquals("Salary credit", result.get(0).getDescription());
    }

    @Test
    void existsByUniqueKey_true_whenMatchFound() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);
        LocalDate txnDate = LocalDate.of(2026, 6, 10);
        BigDecimal amount = new BigDecimal("750.00");

        repository.save(transaction(accountId, uploadId, txnDate, amount, TxnType.DEBIT, "Grocery store"));

        assertTrue(repository.existsByUniqueKey(accountId, txnDate, amount, TxnType.DEBIT, "Grocery store"));
    }

    @Test
    void existsByUniqueKey_false_whenDescriptionDiffers() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);
        LocalDate txnDate = LocalDate.of(2026, 6, 10);
        BigDecimal amount = new BigDecimal("750.00");

        repository.save(transaction(accountId, uploadId, txnDate, amount, TxnType.DEBIT, "Grocery store"));

        assertFalse(repository.existsByUniqueKey(accountId, txnDate, amount, TxnType.DEBIT, "Different description"));
    }

    // ---- Helpers ----

    private UUID saveAccount() {
        Account account = accountRepository.save(Account.builder()
                .accountName("Test Account")
                .accountType(AccountType.SAVINGS)
                .institutionName("Test Bank")
                .openingBalance(BigDecimal.ZERO)
                .build());
        return account.getId();
    }

    private UUID saveUpload(UUID accountId) {
        StatementUpload upload = uploadRepository.save(StatementUpload.builder()
                .accountId(accountId)
                .fileName("test.csv")
                .status(UploadStatus.PENDING)
                .build());
        return upload.getId();
    }

    private Transaction transaction(UUID accountId, UUID uploadId, LocalDate txnDate, BigDecimal amount,
                                    TxnType txnType, String description) {
        return Transaction.builder()
                .accountId(accountId)
                .uploadId(uploadId)
                .txnDate(txnDate)
                .amount(amount)
                .txnType(txnType)
                .description(description)
                .build();
    }
}
