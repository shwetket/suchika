package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.*;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatementUploadServiceTest {

    private StatementUploadService service;
    private FakeUploadRepo uploadRepo;
    private FakeTransactionRepo txnRepo;
    private FakeAccountRepo accountRepo;

    private static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID UPLOAD_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        uploadRepo = new FakeUploadRepo();
        txnRepo = new FakeTransactionRepo();
        accountRepo = new FakeAccountRepo();
        service = new StatementUploadService(uploadRepo, txnRepo, accountRepo, new StatementCsvParser());

        accountRepo.add(Account.builder()
                .id(ACCOUNT_ID)
                .accountName("HDFC Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("HDFC Bank")
                .build());
    }

    @Test
    void upload_happyPath_savesTransactionsAndSucceeds() {
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                01/06/2026,SALARY,,50000.00
                02/06/2026,RENT,20000.00,""";

        StatementUpload result = service.uploadStatement(ACCOUNT_ID, "june.csv", csv);

        assertEquals(UploadStatus.SUCCESS, result.getStatus());
        assertEquals(2, txnRepo.saved.size());
    }

    @Test
    void upload_sameFileDuplicateRows_bothKeptWithSuffix() {
        // Two identical rows in the same CSV upload
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                01/06/2026,DUPLICATE TXNM,500.00,
                01/06/2026,DUPLICATE TXNM,500.00,""";

        service.uploadStatement(ACCOUNT_ID, "dups.csv", csv);

        assertEquals(2, txnRepo.saved.size());
        List<String> descriptions = txnRepo.saved.stream()
                .map(Transaction::getDescription).toList();
        // First row keeps original description; second gets suffix
        assertTrue(descriptions.contains("DUPLICATE TXNM"));
        assertTrue(descriptions.stream().anyMatch(d -> d.contains("#2")));
    }

    @Test
    void upload_crossFileDuplicate_secondUploadSkipsRow() {
        String csv = "Date,Narration,Deposit Amt.\n"
                + "01/06/2026,UNIQUE SALARY,50000.00";

        // First upload succeeds normally
        service.uploadStatement(ACCOUNT_ID, "may.csv", csv);
        assertEquals(1, txnRepo.saved.size());

        // Second upload with the same row — cross-file dedup skips it
        service.uploadStatement(ACCOUNT_ID, "june.csv", csv);
        assertEquals(1, txnRepo.saved.size());  // no new rows added
    }

    @Test
    void upload_accountNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.uploadStatement(UUID.randomUUID(), "test.csv", "Date,Description,Amount\n01/06/2026,test,100"));
    }

    @Test
    void rollback_existingUpload_deletesIt() {
        uploadRepo.store.put(UPLOAD_ID, StatementUpload.builder()
                .id(UPLOAD_ID).accountId(ACCOUNT_ID).fileName("test.csv").status(UploadStatus.SUCCESS).build());

        service.rollbackUpload(UPLOAD_ID);

        assertFalse(uploadRepo.store.containsKey(UPLOAD_ID));
    }

    @Test
    void rollback_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.rollbackUpload(UUID.randomUUID()));
    }

    @Test
    void getUpload_found_returns() {
        uploadRepo.store.put(UPLOAD_ID, StatementUpload.builder()
                .id(UPLOAD_ID).accountId(ACCOUNT_ID).fileName("test.csv").status(UploadStatus.SUCCESS).build());

        StatementUpload result = service.getUpload(UPLOAD_ID);

        assertEquals(UPLOAD_ID, result.getId());
    }

    @Test
    void getUpload_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getUpload(UUID.randomUUID()));
    }

    // ---- Fake repositories ----

    static class FakeUploadRepo implements StatementUploadRepository {
        final Map<UUID, StatementUpload> store = new LinkedHashMap<>();

        @Override
        public StatementUpload save(StatementUpload upload) {
            if (upload.getId() == null) {
                upload = StatementUpload.builder()
                        .id(UUID.randomUUID())
                        .accountId(upload.getAccountId())
                        .fileName(upload.getFileName())
                        .status(upload.getStatus())
                        .build();
            }
            store.put(upload.getId(), upload);
            return upload;
        }

        @Override
        public Optional<StatementUpload> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public StatementUpload updateStatus(UUID id, UploadStatus status) {
            StatementUpload existing = store.get(id);
            existing.setStatus(status);
            return existing;
        }

        @Override
        public List<StatementUpload> findByAccountId(UUID accountId) {
            return store.values().stream()
                    .filter(u -> accountId.equals(u.getAccountId()))
                    .toList();
        }

        @Override
        public void delete(UUID id) {
            store.remove(id);
        }
    }

    static class FakeTransactionRepo implements TransactionRepository {
        final List<Transaction> saved = new ArrayList<>();

        @Override
        public Transaction save(Transaction transaction) {
            Transaction built = Transaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(transaction.getAccountId())
                    .uploadId(transaction.getUploadId())
                    .txnDate(transaction.getTxnDate())
                    .amount(transaction.getAmount())
                    .txnType(transaction.getTxnType())
                    .description(transaction.getDescription())
                    .build();
            saved.add(built);
            return built;
        }

        @Override
        public Optional<Transaction> findById(UUID id) {
            return saved.stream().filter(t -> id.equals(t.getId())).findFirst();
        }

        @Override
        public List<Transaction> findByAccountId(UUID accountId, LocalDate from, LocalDate to, TxnType txnType) {
            return saved.stream().filter(t -> accountId.equals(t.getAccountId())).toList();
        }

        @Override
        public boolean existsByUniqueKey(UUID accountId, LocalDate txnDate, BigDecimal amount, TxnType txnType, String description) {
            return saved.stream().anyMatch(t ->
                    accountId.equals(t.getAccountId())
                    && txnDate.equals(t.getTxnDate())
                    && amount.compareTo(t.getAmount()) == 0
                    && txnType == t.getTxnType()
                    && description.equals(t.getDescription()));
        }
    }

    static class FakeAccountRepo implements AccountRepository {
        private final Map<UUID, Account> store = new HashMap<>();

        void add(Account account) {
            store.put(account.getId(), account);
        }

        @Override
        public Account save(Account account) { store.put(account.getId(), account); return account; }

        @Override
        public Optional<Account> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        @Override
        public List<Account> findAll(UUID profileId, AccountType accountType, Boolean isActive) { return List.copyOf(store.values()); }

        @Override
        public boolean existsById(UUID id) { return store.containsKey(id); }

        @Override
        public boolean hasTransactions(UUID accountId) { return false; }
    }
}
