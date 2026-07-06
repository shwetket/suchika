package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, Month.JUNE, 1);
        BigDecimal amount = new BigDecimal("1500.75");
        Instant createdAt = Instant.parse("2026-06-01T10:00:00Z");
        Map<String, String> metadata = Map.of("ref", "ABC123");

        Transaction txn = Transaction.builder()
                .id(id)
                .accountId(accountId)
                .uploadId(uploadId)
                .txnDate(txnDate)
                .amount(amount)
                .txnType(TxnType.CREDIT)
                .description("Salary credit")
                .metadata(metadata)
                .createdAt(createdAt)
                .build();

        assertEquals(id, txn.getId());
        assertEquals(accountId, txn.getAccountId());
        assertEquals(uploadId, txn.getUploadId());
        assertEquals(txnDate, txn.getTxnDate());
        assertEquals(0, amount.compareTo(txn.getAmount()));
        assertEquals(TxnType.CREDIT, txn.getTxnType());
        assertEquals("Salary credit", txn.getDescription());
        assertEquals(metadata, txn.getMetadata());
        assertEquals(createdAt, txn.getCreatedAt());
    }

    @Test
    void builder_requiredFieldsOnly_optionalFieldsAreNull() {
        UUID accountId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, Month.JANUARY, 15);
        BigDecimal amount = new BigDecimal("200.00");

        Transaction txn = Transaction.builder()
                .accountId(accountId)
                .txnDate(txnDate)
                .amount(amount)
                .txnType(TxnType.DEBIT)
                .description("Grocery purchase")
                .build();

        assertNull(txn.getId());
        assertNull(txn.getUploadId());
        assertNull(txn.getMetadata());
        assertNull(txn.getCreatedAt());

        assertEquals(accountId, txn.getAccountId());
        assertEquals(txnDate, txn.getTxnDate());
        assertEquals(0, amount.compareTo(txn.getAmount()));
        assertEquals(TxnType.DEBIT, txn.getTxnType());
        assertEquals("Grocery purchase", txn.getDescription());
    }

    @Test
    void create_validAmount_buildsTransaction() {
        UUID accountId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, Month.MARCH, 1);
        BigDecimal amount = new BigDecimal("250.50");
        Map<String, String> metadata = Map.of("source", "MANUAL");

        Transaction txn = Transaction.create(accountId, uploadId, txnDate, amount,
                TxnType.DEBIT, "ATM withdrawal", metadata);

        assertEquals(accountId, txn.getAccountId());
        assertEquals(uploadId, txn.getUploadId());
        assertEquals(txnDate, txn.getTxnDate());
        assertEquals(0, amount.compareTo(txn.getAmount()));
        assertEquals(TxnType.DEBIT, txn.getTxnType());
        assertEquals("ATM withdrawal", txn.getDescription());
        assertEquals(metadata, txn.getMetadata());
        assertNull(txn.getId());
        assertNull(txn.getCreatedAt());
    }

    @Test
    void create_zeroAmount_isAllowed() {
        Transaction txn = Transaction.create(UUID.randomUUID(), null,
                LocalDate.of(2026, Month.MARCH, 1), BigDecimal.ZERO, TxnType.CREDIT, "Zero-value entry", null);

        assertEquals(0, BigDecimal.ZERO.compareTo(txn.getAmount()));
    }

    @Test
    void create_negativeAmount_throwsIllegalArgumentException() {
        UUID accountId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, Month.MARCH, 1);
        BigDecimal negative = new BigDecimal("-1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Transaction.create(accountId, null, txnDate, negative, TxnType.DEBIT, "Invalid", null));

        assertTrue(ex.getMessage().contains("amount"));
    }

    @Test
    void create_nullAmount_throwsIllegalArgumentException() {
        UUID accountId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, Month.MARCH, 1);
        assertThrows(IllegalArgumentException.class, () ->
                Transaction.create(accountId, null, txnDate, null, TxnType.DEBIT, "Invalid", null));
    }

    @Test
    void txnType_creditAndDebitEnumValuesExist() {
        assertNotNull(TxnType.CREDIT);
        assertNotNull(TxnType.DEBIT);
        assertEquals(2, TxnType.values().length);
    }

    @Test
    void uploadStatus_pendingSuccessFailedEnumValuesExist() {
        assertNotNull(UploadStatus.PENDING);
        assertNotNull(UploadStatus.SUCCESS);
        assertNotNull(UploadStatus.FAILED);
        assertEquals(3, UploadStatus.values().length);
    }
}
