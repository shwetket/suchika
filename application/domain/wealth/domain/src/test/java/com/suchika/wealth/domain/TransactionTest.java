package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        LocalDate txnDate = LocalDate.of(2026, 6, 1);
        BigDecimal amount = new BigDecimal("1500.75");
        Instant createdAt = Instant.now();
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
        LocalDate txnDate = LocalDate.of(2026, 1, 15);
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
