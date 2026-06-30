package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.ExpenseCategory;
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
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getById(randomId));
    }

    // ---- Epic 8 Phase 2: manual expense category tagging (Use Case 8.3, Q24) ----

    @Test
    void updateCategory_setsCategoryInMetadata() {
        UUID id = UUID.randomUUID();
        Transaction txn = Transaction.builder()
                .id(id)
                .accountId(UUID.randomUUID())
                .txnDate(LocalDate.of(2026, Month.JUNE, 1))
                .amount(new BigDecimal("250.00"))
                .txnType(TxnType.DEBIT)
                .description("Groceries")
                .build();
        repo.store.add(txn);

        Transaction updated = service.updateCategory(id, ExpenseCategory.HOUSEHOLD_CORE);

        assertEquals("HOUSEHOLD_CORE", updated.getMetadata().get("category"));
    }

    @Test
    void updateCategory_nullCategory_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        repo.store.add(Transaction.builder()
                .id(id)
                .accountId(UUID.randomUUID())
                .txnDate(LocalDate.of(2026, Month.JUNE, 1))
                .amount(new BigDecimal("250.00"))
                .txnType(TxnType.DEBIT)
                .description("Groceries")
                .build());

        assertThrows(BadRequestException.class, () -> service.updateCategory(id, null));
    }

    @Test
    void updateCategory_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class,
                () -> service.updateCategory(randomId, ExpenseCategory.DISCRETIONARY));
    }

    @Test
    void bulkUpdateCategory_tagsEveryGivenId() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        repo.store.add(Transaction.builder().id(id1).accountId(accountId)
                .txnDate(LocalDate.of(2026, Month.JUNE, 1)).amount(new BigDecimal("100.00"))
                .txnType(TxnType.DEBIT).description("Item 1").build());
        repo.store.add(Transaction.builder().id(id2).accountId(accountId)
                .txnDate(LocalDate.of(2026, Month.JUNE, 2)).amount(new BigDecimal("200.00"))
                .txnType(TxnType.DEBIT).description("Item 2").build());

        List<Transaction> updated = service.bulkUpdateCategory(List.of(id1, id2), ExpenseCategory.CHILD_RELATED);

        assertEquals(2, updated.size());
        assertTrue(updated.stream().allMatch(t -> "CHILD_RELATED".equals(t.getMetadata().get("category"))));
    }

    @Test
    void bulkUpdateCategory_emptyList_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.bulkUpdateCategory(List.of(), ExpenseCategory.DISCRETIONARY));
    }

    @Test
    void bulkUpdateCategory_nullList_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.bulkUpdateCategory(null, ExpenseCategory.DISCRETIONARY));
    }

    @Test
    void bulkUpdateCategory_unknownId_throwsNotFoundException() {
        List<UUID> ids = List.of(UUID.randomUUID());
        assertThrows(NotFoundException.class,
                () -> service.bulkUpdateCategory(ids, ExpenseCategory.DISCRETIONARY));
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
        public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
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
        public boolean existsByDeduplicationKey(UUID accountId, UUID profileId, LocalDate txnDate, BigDecimal amount, TxnType txnType) {
            return false;
        }

        @Override
        public BigDecimal sumAmountByTxnType(UUID accountId, UUID profileId, TxnType txnType) {
            return store.stream()
                    .filter(t -> accountId.equals(t.getAccountId()) && txnType == t.getTxnType())
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
