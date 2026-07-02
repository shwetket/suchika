package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.BulkUpdateTransactionCategoryRequest;
import com.suchika.wealth.adapters.http.dto.CreateTransactionRequest;
import com.suchika.wealth.adapters.http.dto.ListTransactionsResponse;
import com.suchika.wealth.adapters.http.dto.TransactionResponse;
import com.suchika.wealth.adapters.http.dto.UpdateTransactionCategoryRequest;
import com.suchika.wealth.domain.ExpenseCategory;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.CreateTransactionCommand;
import com.suchika.wealth.ports.input.PagedTransactions;
import com.suchika.wealth.ports.input.TransactionUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionResourceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID TXN_ID = UUID.randomUUID();

    private TransactionResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new TransactionResource(useCase);
    }

    @Test
    void listTransactions_returns200_withTransactionList() {
        useCase.transactionsToReturn = List.of(buildTransaction());
        useCase.totalCountToReturn = 1;

        Response response = resource.listTransactions(
                ACCOUNT_ID, PROFILE_ID, "2026-01-01", "2026-01-31", "CREDIT", null, null);

        assertEquals(200, response.getStatus());
        ListTransactionsResponse body = (ListTransactionsResponse) response.getEntity();
        assertEquals(1, body.transactions.size());
        assertEquals(1, body.totalSize);
        assertEquals(PROFILE_ID, useCase.lastListProfileId);
        assertEquals(LocalDate.of(2026, 1, 1), useCase.lastListFrom);
        assertEquals(TxnType.CREDIT, useCase.lastListTxnType);
    }

    @Test
    void listTransactions_defaultsPageAndSizeWhenNotProvided() {
        useCase.transactionsToReturn = List.of(buildTransaction());

        Response response = resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, null, null, null);

        ListTransactionsResponse body = (ListTransactionsResponse) response.getEntity();
        assertEquals(0, body.page);
        assertEquals(50, body.size);
        assertEquals(0, useCase.lastListPage);
        assertEquals(50, useCase.lastListSize);
    }

    @Test
    void listTransactions_honorsExplicitPageAndSize() {
        useCase.transactionsToReturn = List.of(buildTransaction());
        useCase.totalCountToReturn = 120;

        Response response = resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, null, 2, 25);

        ListTransactionsResponse body = (ListTransactionsResponse) response.getEntity();
        assertEquals(2, body.page);
        assertEquals(25, body.size);
        assertEquals(120, body.totalSize);
        assertEquals(2, useCase.lastListPage);
        assertEquals(25, useCase.lastListSize);
    }

    @Test
    void listTransactions_negativePage_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, null, -1, null));
    }

    @Test
    void listTransactions_sizeExceedsMax_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, null, null, 500));
    }

    @Test
    void listTransactions_sizeZero_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, null, null, 0));
    }

    @Test
    void listTransactions_invalidFromDate_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listTransactions(ACCOUNT_ID, PROFILE_ID, "not-a-date", null, null, null, null));
    }

    @Test
    void listTransactions_invalidTxnType_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listTransactions(ACCOUNT_ID, PROFILE_ID, null, null, "NOT_A_TYPE", null, null));
    }

    @Test
    void createTransaction_returns201_withCreatedTransaction() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.txnDate = "2026-02-01";
        request.amount = BigDecimal.valueOf(500);
        request.txnType = "DEBIT";
        request.description = "Groceries";
        useCase.transactionToReturn = buildTransaction();

        Response response = resource.createTransaction(ACCOUNT_ID, PROFILE_ID, request);

        assertEquals(201, response.getStatus());
        assertEquals(ACCOUNT_ID, useCase.lastCreateAccountId);
        assertEquals(PROFILE_ID, useCase.lastCreateProfileId);
        assertEquals(TxnType.DEBIT, useCase.lastCreateCommand.txnType());
    }

    @Test
    void createTransaction_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.createTransaction(ACCOUNT_ID, PROFILE_ID, null));
    }

    @Test
    void createTransaction_missingTxnType_throwsBadRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.txnDate = "2026-02-01";
        request.amount = BigDecimal.TEN;
        assertThrows(BadRequestException.class, () -> resource.createTransaction(ACCOUNT_ID, PROFILE_ID, request));
    }

    @Test
    void createTransaction_missingTxnDate_throwsBadRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.txnType = "CREDIT";
        request.amount = BigDecimal.TEN;
        assertThrows(BadRequestException.class, () -> resource.createTransaction(ACCOUNT_ID, PROFILE_ID, request));
    }

    @Test
    void getTransaction_returnsTransactionResponse() {
        useCase.transactionToReturn = buildTransaction();

        TransactionResponse response = resource.getTransaction(ACCOUNT_ID, TXN_ID);

        assertEquals("Groceries", response.description);
    }

    @Test
    void updateCategory_returnsUpdatedTransaction() {
        UpdateTransactionCategoryRequest request = new UpdateTransactionCategoryRequest();
        request.category = "HOUSEHOLD_CORE";
        useCase.transactionToReturn = buildTransaction();

        TransactionResponse response = resource.updateCategory(ACCOUNT_ID, TXN_ID, request);

        assertEquals("Groceries", response.description);
        assertEquals(TXN_ID, useCase.lastUpdateCategoryTxnId);
        assertEquals(ExpenseCategory.HOUSEHOLD_CORE, useCase.lastUpdateCategoryValue);
    }

    @Test
    void updateCategory_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateCategory(ACCOUNT_ID, TXN_ID, null));
    }

    @Test
    void updateCategory_invalidCategory_throwsBadRequest() {
        UpdateTransactionCategoryRequest request = new UpdateTransactionCategoryRequest();
        request.category = "NOT_A_CATEGORY";
        assertThrows(BadRequestException.class, () -> resource.updateCategory(ACCOUNT_ID, TXN_ID, request));
    }

    @Test
    void bulkUpdateCategory_returns200_withUpdatedTransactions() {
        BulkUpdateTransactionCategoryRequest request = new BulkUpdateTransactionCategoryRequest();
        request.transactionIds = List.of(TXN_ID);
        request.category = "DISCRETIONARY";
        useCase.bulkUpdateResult = List.of(buildTransaction());

        Response response = resource.bulkUpdateCategory(ACCOUNT_ID, request);

        assertEquals(200, response.getStatus());
        ListTransactionsResponse body = (ListTransactionsResponse) response.getEntity();
        assertEquals(1, body.transactions.size());
        assertEquals(ExpenseCategory.DISCRETIONARY, useCase.lastBulkCategory);
        assertEquals(List.of(TXN_ID), useCase.lastBulkIds);
    }

    @Test
    void bulkUpdateCategory_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.bulkUpdateCategory(ACCOUNT_ID, null));
    }

    private Transaction buildTransaction() {
        return Transaction.builder()
                .id(TXN_ID)
                .accountId(ACCOUNT_ID)
                .txnDate(LocalDate.of(2026, 2, 1))
                .amount(BigDecimal.valueOf(500))
                .txnType(TxnType.DEBIT)
                .description("Groceries")
                .build();
    }

    static class StubUseCase implements TransactionUseCase {
        List<Transaction> transactionsToReturn = List.of();
        long totalCountToReturn;
        Transaction transactionToReturn;
        List<Transaction> bulkUpdateResult = List.of();

        UUID lastListProfileId;
        LocalDate lastListFrom;
        TxnType lastListTxnType;
        Integer lastListPage;
        Integer lastListSize;
        UUID lastCreateAccountId;
        UUID lastCreateProfileId;
        CreateTransactionCommand lastCreateCommand;
        UUID lastUpdateCategoryTxnId;
        ExpenseCategory lastUpdateCategoryValue;
        List<UUID> lastBulkIds;
        ExpenseCategory lastBulkCategory;

        @Override
        public List<Transaction> listByAccount(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
            lastListProfileId = profileId;
            lastListFrom = from;
            lastListTxnType = txnType;
            return transactionsToReturn;
        }

        @Override
        public PagedTransactions listByAccountPaginated(UUID accountId, UUID profileId, LocalDate from, LocalDate to,
                                                          TxnType txnType, int page, int size) {
            lastListProfileId = profileId;
            lastListFrom = from;
            lastListTxnType = txnType;
            lastListPage = page;
            lastListSize = size;
            return new PagedTransactions(transactionsToReturn, totalCountToReturn);
        }

        @Override
        public Transaction create(UUID accountId, UUID profileId, CreateTransactionCommand command) {
            lastCreateAccountId = accountId;
            lastCreateProfileId = profileId;
            lastCreateCommand = command;
            return transactionToReturn;
        }

        @Override
        public Transaction getById(UUID id) {
            return transactionToReturn;
        }

        @Override
        public Transaction updateCategory(UUID id, ExpenseCategory category) {
            lastUpdateCategoryTxnId = id;
            lastUpdateCategoryValue = category;
            return transactionToReturn;
        }

        @Override
        public List<Transaction> bulkUpdateCategory(List<UUID> ids, ExpenseCategory category) {
            lastBulkIds = ids;
            lastBulkCategory = category;
            return bulkUpdateResult;
        }
    }
}
