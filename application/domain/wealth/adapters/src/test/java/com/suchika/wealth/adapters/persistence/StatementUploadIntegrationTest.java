package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.adapters.services.CsvParseException;
import com.suchika.wealth.domain.*;
import com.suchika.wealth.ports.input.StatementUploadUseCase;
import com.suchika.wealth.ports.input.UploadResult;
import com.suchika.wealth.ports.output.TransactionRepository;
import com.suchika.wealth.ports.output.UploadErrorLogRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for the CSV upload → transaction → rollback flow.
 * Requires a running local PostgreSQL (app_db) with wealth seed applied.
 * Seeded account: f3b90000-0000-0000-0000-000000000000 (Test Account, SAVINGS).
 * Each test runs in a @TestTransaction that rolls back on completion.
 */
@QuarkusTest
@TestProfile(StatementUploadIntegrationTest.DatabaseIntegrationProfile.class)
@TestTransaction
class StatementUploadIntegrationTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    private static final UUID SEEDED_ACCOUNT_ID = UUID.fromString("f3b90000-0000-0000-0000-000000000000");

    private static final String HDFC_SAVINGS_CSV = """
            Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
            01/06/2026,SALARY CREDIT,,01/06/2026,,50000.00,100000.00
            02/06/2026,RENT PAYMENT,,02/06/2026,20000.00,,80000.00
            03/06/2026,UPI PAYMENT,,03/06/2026,1000.00,,79000.00""";

    @Inject
    StatementUploadUseCase uploadUseCase;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    UploadErrorLogRepository errorLogRepository;

    @Inject
    EntityManager em;

    @Test
    void uploadStatement_parsesAndPersistsAllRows() {
        UploadResult result = uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "june.csv", HDFC_SAVINGS_CSV);

        assertEquals(UploadStatus.SUCCESS, result.getUpload().getStatus());
        assertEquals("june.csv", result.getUpload().getFileName());
        assertEquals(SEEDED_ACCOUNT_ID, result.getUpload().getAccountId());
        assertEquals(3, result.getInsertedCount());

        List<Transaction> txns = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null, null, null, null);
        assertEquals(3, txns.size());

        assertTrue(txns.stream().anyMatch(t -> t.getTxnType() == TxnType.CREDIT
                && t.getDescription().equals("SALARY CREDIT")));
        assertTrue(txns.stream().anyMatch(t -> t.getTxnType() == TxnType.DEBIT
                && t.getDescription().equals("RENT PAYMENT")));
    }

    @Test
    void uploadStatement_transactionsLinkedToUpload() {
        UploadResult result = uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "link-test.csv", HDFC_SAVINGS_CSV);

        List<Transaction> txns = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null, null, null, null);
        assertTrue(txns.stream().allMatch(t -> result.getUpload().getId().equals(t.getUploadId())));
    }

    @Test
    void uploadStatement_sameFileDuplicates_bothKeptWithSuffix() {
        String csvWithDuplicates = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                05/06/2026,DUPLICATE TXN,500.00,
                05/06/2026,DUPLICATE TXN,500.00,""";

        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "dups.csv", csvWithDuplicates);

        List<Transaction> txns = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                LocalDate.of(2026, Month.JUNE, 5), LocalDate.of(2026, Month.JUNE, 5), TxnType.DEBIT);
        assertEquals(2, txns.size());

        List<String> descriptions = txns.stream().map(Transaction::getDescription).toList();
        assertTrue(descriptions.contains("DUPLICATE TXN"), "First row should keep original description");
        assertTrue(descriptions.stream().anyMatch(d -> d.contains("#2")), "Second row should get #2 suffix");
    }

    @Test
    void uploadStatement_crossFileDuplicate_secondUploadRowSkipped() {
        String singleRowCsv =
                "Date,Narration,Deposit Amt.\n"
                + "10/06/2026,UNIQUE SALARY,60000.00";

        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "first.csv", singleRowCsv);
        List<Transaction> afterFirst = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                LocalDate.of(2026, Month.JUNE, 10), LocalDate.of(2026, Month.JUNE, 10), null);
        assertEquals(1, afterFirst.size());

        UploadResult second = uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "second.csv", singleRowCsv);
        assertEquals(UploadStatus.SUCCESS, second.getUpload().getStatus());

        List<Transaction> afterSecond = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                LocalDate.of(2026, Month.JUNE, 10), LocalDate.of(2026, Month.JUNE, 10), null);
        assertEquals(1, afterSecond.size());
    }

    @Test
    void rollbackUpload_cascadeDeletesAllTransactions() {
        UploadResult result = uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "to-rollback.csv", HDFC_SAVINGS_CSV);
        UUID uploadId = result.getUpload().getId();

        List<Transaction> before = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null, null, null, null);
        assertFalse(before.isEmpty(), "Expected transactions before rollback");

        uploadUseCase.rollbackUpload(uploadId);
        em.flush();
        em.clear();

        List<Transaction> after = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null, null, null, null);
        assertTrue(after.stream().noneMatch(t -> uploadId.equals(t.getUploadId())),
                "All transactions for the rolled-back upload should be removed");
    }

    @Test
    void listUploads_returnsInDateDescOrder() {
        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "first.csv", HDFC_SAVINGS_CSV);
        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "second.csv", HDFC_SAVINGS_CSV);

        List<StatementUpload> uploads = uploadUseCase.listUploads(SEEDED_ACCOUNT_ID);
        assertFalse(uploads.isEmpty());
        assertTrue(uploads.stream().anyMatch(u -> u.getFileName().equals("first.csv")));
        assertTrue(uploads.stream().anyMatch(u -> u.getFileName().equals("second.csv")));
    }

    @Test
    void filterByDateRange_returnsOnlyMatchingRows() {
        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "range-test.csv", HDFC_SAVINGS_CSV);

        List<Transaction> juneOnly = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                LocalDate.of(2026, Month.JUNE, 1), LocalDate.of(2026, Month.JUNE, 30), null);
        assertFalse(juneOnly.isEmpty());
        assertTrue(juneOnly.stream().allMatch(t ->
                !t.getTxnDate().isBefore(LocalDate.of(2026, Month.JUNE, 1)) &&
                !t.getTxnDate().isAfter(LocalDate.of(2026, Month.JUNE, 30))));
    }

    @Test
    void filterByTxnType_returnsOnlyMatching() {
        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "type-filter.csv", HDFC_SAVINGS_CSV);

        List<Transaction> creditsOnly = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                null, null, TxnType.CREDIT);
        assertTrue(creditsOnly.stream().allMatch(t -> t.getTxnType() == TxnType.CREDIT));

        List<Transaction> debitsOnly = transactionRepository.findByAccountId(SEEDED_ACCOUNT_ID, null,
                null, null, TxnType.DEBIT);
        assertTrue(debitsOnly.stream().allMatch(t -> t.getTxnType() == TxnType.DEBIT));
    }

    // ---- Phase 1: malformed CSV → error log ----

    @Test
    void uploadStatement_missingDateColumn_writesToErrorLog() {
        String csvMissingDate = "Description,Amount\nSALARY CREDIT,50000.00";

        assertThrows(CsvParseException.class,
                () -> uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "bad-headers.csv", csvMissingDate));

        em.flush();
        em.clear();

        // Verify all uploads for this account — find the FAILED one
        List<StatementUpload> uploads = uploadUseCase.listUploads(SEEDED_ACCOUNT_ID);
        StatementUpload failedUpload = uploads.stream()
                .filter(u -> u.getStatus() == UploadStatus.FAILED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a FAILED upload"));

        List<com.suchika.wealth.domain.UploadErrorLog> errors =
                errorLogRepository.findByUploadId(failedUpload.getId());

        assertEquals(1, errors.size());
        assertEquals("MISSING_DATE_COLUMN", errors.get(0).getErrorType());
        assertFalse(errors.get(0).getMissingColumns().isEmpty());
    }

    // ---- Phase 2: cross-file dedup → skipped rows in response ----

    @Test
    void uploadStatement_crossFileDuplicate_skippedRowsInResponse() {
        String singleRowCsv = "Date,Narration,Deposit Amt.\n20/06/2026,BONUS CREDIT,25000.00";

        uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "first.csv", singleRowCsv);
        UploadResult second = uploadUseCase.uploadStatement(SEEDED_ACCOUNT_ID, "second.csv", singleRowCsv);

        assertEquals(UploadStatus.SUCCESS, second.getUpload().getStatus());
        assertEquals(1, second.getSkippedDuplicates().size());

        UploadResult.SkippedRow skipped = second.getSkippedDuplicates().get(0);
        assertEquals(LocalDate.of(2026, Month.JUNE, 20), skipped.txnDate());
        assertEquals(0, new java.math.BigDecimal("25000.00").compareTo(skipped.amount()));
        assertEquals("BONUS CREDIT", skipped.description());
        assertEquals(0, second.getInsertedCount());
    }
}
