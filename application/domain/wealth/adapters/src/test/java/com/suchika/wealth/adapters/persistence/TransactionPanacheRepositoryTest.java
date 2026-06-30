package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.*;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
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

    @Inject
    EntityManager em;

    @Test
    void save_andFindById_roundTrip() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        Transaction saved = repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.JUNE, 1),
                new BigDecimal("1000.00"), TxnType.CREDIT, "Salary"));

        assertNotNull(saved.getId());
        assertEquals(accountId, saved.getAccountId());
        assertEquals(uploadId, saved.getUploadId());
        assertEquals(LocalDate.of(2026, Month.JUNE, 1), saved.getTxnDate());
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

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.MARCH, 1),
                new BigDecimal("500.00"), TxnType.DEBIT, "Rent"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.APRIL, 1),
                new BigDecimal("200.00"), TxnType.DEBIT, "Electricity"));

        List<Transaction> result = repository.findByAccountId(accountId, null, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void findByAccountId_fromFilter_excludesOlderRows() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.JANUARY, 1),
                new BigDecimal("100.00"), TxnType.DEBIT, "January bill"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.JUNE, 1),
                new BigDecimal("200.00"), TxnType.DEBIT, "June bill"));

        List<Transaction> result = repository.findByAccountId(accountId, null, LocalDate.of(2026, Month.JUNE, 1), null, null);

        assertEquals(1, result.size());
        assertEquals("June bill", result.get(0).getDescription());
    }

    @Test
    void findByAccountId_toFilter_excludesNewerRows() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.JANUARY, 15),
                new BigDecimal("150.00"), TxnType.DEBIT, "January purchase"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.FEBRUARY, 10),
                new BigDecimal("300.00"), TxnType.DEBIT, "February purchase"));

        List<Transaction> result = repository.findByAccountId(accountId, null, null, LocalDate.of(2026, Month.JANUARY, 31), null);

        assertEquals(1, result.size());
        assertEquals("January purchase", result.get(0).getDescription());
    }

    @Test
    void findByAccountId_txnTypeFilter_returnsOnlyMatching() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.MAY, 1),
                new BigDecimal("5000.00"), TxnType.CREDIT, "Salary credit"));
        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.MAY, 5),
                new BigDecimal("200.00"), TxnType.DEBIT, "ATM withdrawal"));

        List<Transaction> result = repository.findByAccountId(accountId, null, null, null, TxnType.CREDIT);

        assertEquals(1, result.size());
        assertEquals(TxnType.CREDIT, result.get(0).getTxnType());
        assertEquals("Salary credit", result.get(0).getDescription());
    }

    @Test
    void existsByDeduplicationKey_true_whenMatchFound() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);
        LocalDate txnDate = LocalDate.of(2026, Month.JUNE, 10);
        BigDecimal amount = new BigDecimal("750.00");

        repository.save(transaction(accountId, uploadId, txnDate, amount, TxnType.DEBIT, "Grocery store"));

        assertTrue(repository.existsByDeduplicationKey(accountId, null, txnDate, amount, TxnType.DEBIT));
    }

    @Test
    void existsByDeduplicationKey_false_whenTxnTypeDiffers() {
        UUID accountId = saveAccount();
        UUID uploadId = saveUpload(accountId);
        LocalDate txnDate = LocalDate.of(2026, Month.JUNE, 10);
        BigDecimal amount = new BigDecimal("750.00");

        repository.save(transaction(accountId, uploadId, txnDate, amount, TxnType.DEBIT, "Grocery store"));

        assertFalse(repository.existsByDeduplicationKey(accountId, null, txnDate, amount, TxnType.CREDIT));
    }

    // ---- Bug 4 fix: profile_id scoping (ADR-006) ----

    @Test
    void findByAccountId_profileFilter_blocksCrossProfileAccess() {
        UUID ownerProfileId = saveProfile("Owner Member");
        UUID otherProfileId = saveProfile("Other Member");

        UUID accountId = saveAccountForProfile(ownerProfileId);
        UUID uploadId = saveUpload(accountId);

        repository.save(transaction(accountId, uploadId, LocalDate.of(2026, Month.JUNE, 15),
                new BigDecimal("999.00"), TxnType.DEBIT, "Owner's transaction"));

        List<Transaction> asOwner = repository.findByAccountId(accountId, ownerProfileId, null, null, null);
        assertEquals(1, asOwner.size(), "Owner profile should see the transaction on their own account");

        List<Transaction> asOtherProfile = repository.findByAccountId(accountId, otherProfileId, null, null, null);
        assertTrue(asOtherProfile.isEmpty(),
                "A different profile_id must not see transactions on an account it does not own");
    }

    @Test
    void existsByDeduplicationKey_profileFilter_blocksCrossProfileMatch() {
        UUID ownerProfileId = saveProfile("Owner Member 2");
        UUID otherProfileId = saveProfile("Other Member 2");

        UUID accountId = saveAccountForProfile(ownerProfileId);
        UUID uploadId = saveUpload(accountId);

        LocalDate txnDate = LocalDate.of(2026, Month.JULY, 1);
        BigDecimal amount = new BigDecimal("321.00");
        repository.save(transaction(accountId, uploadId, txnDate, amount, TxnType.DEBIT, "Owner's purchase"));

        assertTrue(repository.existsByDeduplicationKey(accountId, ownerProfileId, txnDate, amount, TxnType.DEBIT),
                "Owner profile should match the existing dedup key on their own account");
        assertFalse(repository.existsByDeduplicationKey(accountId, otherProfileId, txnDate, amount, TxnType.DEBIT),
                "A different profile_id must not match a dedup key on an account it does not own");
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

    private UUID saveAccountForProfile(UUID profileId) {
        Account account = accountRepository.save(Account.builder()
                .profileId(profileId)
                .accountName("Profile-Scoped Account")
                .accountType(AccountType.SAVINGS)
                .institutionName("Test Bank")
                .openingBalance(BigDecimal.ZERO)
                .build());
        return account.getId();
    }

    /**
     * Inserts a minimal profile.admin + profile.profile row pair via native SQL
     * so account.profile_id (FK to profile.profile) is satisfiable in tests.
     * Rolled back automatically by @TestTransaction.
     */
    private UUID saveProfile(String fullName) {
        UUID adminId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.admin (id, display_name) VALUES (?1, ?2)")
                .setParameter(1, adminId)
                .setParameter(2, "Test Admin")
                .executeUpdate();

        UUID profileId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin) "
                        + "VALUES (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, profileId)
                .setParameter(2, adminId)
                .setParameter(3, fullName)
                .setParameter(4, LocalDate.of(1990, Month.JANUARY, 1))
                .setParameter(5, "OTHER")
                .executeUpdate();

        return profileId;
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
