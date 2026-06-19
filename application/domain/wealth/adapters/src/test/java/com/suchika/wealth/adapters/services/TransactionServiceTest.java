package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.output.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService service;
    private FakeTransactionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FakeTransactionRepository();
        service = new TransactionService(repo);
    }

    @Test
    void listByAccount_delegatesToRepoWithAllFilterParams() {
        UUID accountId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate to = LocalDate.of(2026, Month.JUNE, 30);

        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .txnDate(LocalDate.of(2026, Month.MARCH, 15))
                .amount(new BigDecimal("500.00"))
                .txnType(TxnType.CREDIT)
                .description("Test credit")
                .build();
        repo.store.add(txn);

        List<Transaction> result = service.listByAccount(accountId, from, to, TxnType.CREDIT);

        assertEquals(1, result.size());
        assertEquals(txn.getId(), result.get(0).getId());
        assertEquals(accountId, repo.lastAccountId);
        assertEquals(from, repo.lastFrom);
        assertEquals(to, repo.lastTo);
        assertEquals(TxnType.CREDIT, repo.lastTxnType);
    }

    @Test
    void listByAccount_nullFilters_passThroughToRepo() {
        UUID accountId = UUID.randomUUID();

        service.listByAccount(accountId, null, null, null);

        assertEquals(accountId, repo.lastAccountId);
        assertNull(repo.lastFrom);
        assertNull(repo.lastTo);
        assertNull(repo.lastTxnType);
    }

    @Test
    void getById_found_returnsTransaction() {
        UUID id = UUID.randomUUID();
        Transaction txn = Transaction.builder()
                .id(id)
                .accountId(UUID.randomUUID())
                .txnDate(LocalDate.of(2026, Month.JUNE, 1))
                .amount(new BigDecimal("100.00"))
                .txnType(TxnType.DEBIT)
                .description("ATM withdrawal")
                .build();
        repo.store.add(txn);

        Transaction found = service.getById(id);

        assertEquals(id, found.getId());
        assertEquals("ATM withdrawal", found.getDescription());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    // ---- Fake repository ----

    static class FakeTransactionRepository implements TransactionRepository {
        final List<Transaction> store = new ArrayList<>();
        UUID lastAccountId;
        LocalDate lastFrom;
        LocalDate lastTo;
        TxnType lastTxnType;

        @Override
        public Transaction save(Transaction transaction) {
            if (transaction.getId() == null) {
                transaction = Transaction.builder()
                        .id(UUID.randomUUID())
                        .accountId(transaction.getAccountId())
                        .uploadId(transaction.getUploadId())
                        .txnDate(transaction.getTxnDate())
                        .amount(transaction.getAmount())
                        .txnType(transaction.getTxnType())
                        .description(transaction.getDescription())
                        .metadata(transaction.getMetadata())
                        .createdAt(transaction.getCreatedAt())
                        .build();
            }
            store.add(transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findById(UUID id) {
            return store.stream().filter(t -> id.equals(t.getId())).findFirst();
        }

        @Override
        public List<Transaction> findByAccountId(UUID accountId, LocalDate from, LocalDate to, TxnType txnType) {
            this.lastAccountId = accountId;
            this.lastFrom = from;
            this.lastTo = to;
            this.lastTxnType = txnType;
            return store.stream()
                    .filter(t -> accountId.equals(t.getAccountId()))
                    .filter(t -> from == null || !t.getTxnDate().isBefore(from))
                    .filter(t -> to == null || !t.getTxnDate().isAfter(to))
                    .filter(t -> txnType == null || txnType.equals(t.getTxnType()))
                    .toList();
        }

        @Override
        public boolean existsByUniqueKey(UUID accountId, LocalDate txnDate, BigDecimal amount, TxnType txnType, String description) {
            return false;
        }
    }
}
