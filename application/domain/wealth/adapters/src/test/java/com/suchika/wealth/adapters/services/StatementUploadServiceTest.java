package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.*;
import com.suchika.wealth.ports.input.UploadResult;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import com.suchika.wealth.ports.output.UploadErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatementUploadServiceTest {

    private StatementUploadService service;
    private FakeUploadRepo uploadRepo;
    private FakeTransactionRepo txnRepo;
    private FakeAccountRepo accountRepo;
    private FakeErrorLogRepo errorLogRepo;

    private static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID UPLOAD_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        uploadRepo = new FakeUploadRepo();
        txnRepo = new FakeTransactionRepo();
        accountRepo = new FakeAccountRepo();
        errorLogRepo = new FakeErrorLogRepo();
        service = new StatementUploadService(uploadRepo, txnRepo, accountRepo, new StatementCsvParser(), errorLogRepo);

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

        UploadResult result = service.uploadStatement(ACCOUNT_ID, "june.csv", csv);

        assertEquals(UploadStatus.SUCCESS, result.getUpload().getStatus());
        assertEquals(2, txnRepo.saved.size());
        assertEquals(2, result.getInsertedCount());
        assertTrue(result.getSkippedDuplicates().isEmpty());
    }

    @Test
    void upload_sameFileDuplicateRows_bothKeptWithSuffix() {
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                01/06/2026,DUPLICATE TXNM,500.00,
                01/06/2026,DUPLICATE TXNM,500.00,""";

        service.uploadStatement(ACCOUNT_ID, "dups.csv", csv);

        assertEquals(2, txnRepo.saved.size());
        List<String> descriptions = txnRepo.saved.stream()
                .map(Transaction::getDescription).toList();
        assertTrue(descriptions.contains("DUPLICATE TXNM"));
        assertTrue(descriptions.stream().anyMatch(d -> d.contains("#2")));
    }

    @Test
    void upload_crossFileDuplicate_secondUploadSkipsRow() {
        String csv = "Date,Narration,Deposit Amt.\n"
                + "01/06/2026,UNIQUE SALARY,50000.00";

        service.uploadStatement(ACCOUNT_ID, "may.csv", csv);
        assertEquals(1, txnRepo.saved.size());

        service.uploadStatement(ACCOUNT_ID, "june.csv", csv);
        assertEquals(1, txnRepo.saved.size());
    }

    @Test
    void upload_accountNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.uploadStatement(UUID.randomUUID(), "test.csv",
                        "Date,Description,Amount\n01/06/2026,test,100"));
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

    // ---- Phase 1: CsvParseException → error log + FAILED status ----

    @Test
    void uploadStatement_csvParseException_writesToErrorLog() {
        String csvMissingDate = "Description,Amount\nSome Transaction,500.00";

        assertThrows(CsvParseException.class,
                () -> service.uploadStatement(ACCOUNT_ID, "bad.csv", csvMissingDate));

        assertEquals(1, errorLogRepo.savedEntries.size());
        FakeErrorLogRepo.SavedEntry entry = errorLogRepo.savedEntries.get(0);
        assertEquals("MISSING_DATE_COLUMN", entry.errorType());
        assertFalse(entry.missingColumns().isEmpty());
        assertNotNull(entry.errorDetail());
    }

    @Test
    void uploadStatement_csvParseException_transitionsToFailed() {
        String csvMissingDate = "Description,Amount\nSome Transaction,500.00";

        assertThrows(CsvParseException.class,
                () -> service.uploadStatement(ACCOUNT_ID, "bad.csv", csvMissingDate));

        assertTrue(uploadRepo.store.values().stream()
                .anyMatch(u -> u.getStatus() == UploadStatus.FAILED));
    }

    // ---- Phase 2: skipped rows in response ----

    @Test
    void uploadStatement_crossFileDuplicate_appearsInSkippedList() {
        String csv = "Date,Narration,Deposit Amt.\n10/06/2026,SALARY,60000.00";

        service.uploadStatement(ACCOUNT_ID, "first.csv", csv);
        UploadResult second = service.uploadStatement(ACCOUNT_ID, "second.csv", csv);

        assertEquals(1, second.getSkippedDuplicates().size());
        UploadResult.SkippedRow skipped = second.getSkippedDuplicates().get(0);
        assertEquals(LocalDate.of(2026, Month.JUNE, 10), skipped.txnDate());
        assertEquals(0, new BigDecimal("60000.00").compareTo(skipped.amount()));
    }

    @Test
    void uploadStatement_crossFileDuplicate_usesNewDedupKey() {
        // Verify dedup uses 4-field key (no description): two rows with same
        // date/amount/type but different descriptions should both be skipped
        // if the 4-field key matches an existing row.
        String firstCsv = "Date,Narration,Deposit Amt.\n15/06/2026,ORIGINAL DESC,1000.00";
        String secondCsv = "Date,Narration,Deposit Amt.\n15/06/2026,DIFFERENT DESC,1000.00";

        service.uploadStatement(ACCOUNT_ID, "first.csv", firstCsv);
        UploadResult result = service.uploadStatement(ACCOUNT_ID, "second.csv", secondCsv);

        // With 4-field dedup (no description), the second row is a duplicate
        assertEquals(1, result.getSkippedDuplicates().size(),
                "Row with same date/amount/type but different description should be skipped");
        assertEquals(0, result.getInsertedCount());
    }

    @Test
    void getUploadErrors_delegatesToRepo() {
        UUID uploadId = UUID.randomUUID();
        errorLogRepo.savedEntries.add(new FakeErrorLogRepo.SavedEntry(
                uploadId, "MISSING_DATE_COLUMN", List.of("date"), "detail"));

        List<UploadErrorLog> errors = service.getUploadErrors(uploadId);

        assertEquals(1, errors.size());
        assertEquals("MISSING_DATE_COLUMN", errors.get(0).getErrorType());
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
        public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
            return saved.stream().filter(t -> accountId.equals(t.getAccountId())).toList();
        }

        @Override
        public boolean existsByDeduplicationKey(UUID accountId, UUID profileId, LocalDate txnDate, BigDecimal amount, TxnType txnType) {
            return saved.stream().anyMatch(t ->
                    accountId.equals(t.getAccountId())
                    && txnDate.equals(t.getTxnDate())
                    && amount.compareTo(t.getAmount()) == 0
                    && txnType == t.getTxnType());
        }

        @Override
        public BigDecimal sumAmountByTxnType(UUID accountId, UUID profileId, TxnType txnType) {
            return saved.stream()
                    .filter(t -> accountId.equals(t.getAccountId()) && txnType == t.getTxnType())
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    static class FakeErrorLogRepo implements UploadErrorLogRepository {
        final List<SavedEntry> savedEntries = new ArrayList<>();

        record SavedEntry(UUID uploadId, String errorType, List<String> missingColumns, String errorDetail) {}

        @Override
        public void save(UUID uploadId, String errorType, List<String> missingColumns, String errorDetail) {
            savedEntries.add(new SavedEntry(uploadId, errorType, missingColumns, errorDetail));
        }

        @Override
        public List<UploadErrorLog> findByUploadId(UUID uploadId) {
            return savedEntries.stream()
                    .filter(e -> uploadId.equals(e.uploadId()))
                    .map(e -> UploadErrorLog.builder()
                            .uploadId(e.uploadId())
                            .errorType(e.errorType())
                            .missingColumns(e.missingColumns())
                            .errorDetail(e.errorDetail())
                            .build())
                    .toList();
        }
    }
}
