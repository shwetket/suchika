package com.suchika.wealth.adapters.http;

import com.suchika.wealth.adapters.http.dto.AccountBalanceResponse;
import com.suchika.wealth.adapters.http.dto.AccountResponse;
import com.suchika.wealth.adapters.http.dto.CreateAccountRequest;
import com.suchika.wealth.adapters.http.dto.CreateTransactionRequest;
import com.suchika.wealth.adapters.http.dto.TransactionResponse;
import com.suchika.wealth.adapters.services.AccountService;
import com.suchika.wealth.adapters.services.TransactionService;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import com.suchika.shared.exception.BadRequestException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * True end-to-end integration test: account creation -> manual transaction entry ->
 * balance calculation, exercising the real HTTP resources, real services, real Panache
 * repositories, and real PostgreSQL. Contrast with AccountResourceTest/TransactionResourceTest,
 * which construct the Resource with a hand-written stub use-case and never touch the DB.
 * This test wires the real, DI-managed services and repositories, then calls Resource
 * methods directly (same calling convention as the existing *ResourceTest classes).
 * <p>
 * Requires a running local PostgreSQL (app_db) with the wealth + profile schemas migrated.
 * Uses the seeded profile (R__seed_profile_test_data.sql) as the FK target for profile_id,
 * and creates its own Account/Transaction rows so it never assumes an empty table.
 * Runs in a transaction that is rolled back on completion (@TestTransaction).
 */
@QuarkusTest
@TestTransaction
class AccountTransactionBalanceIT {

    // Seeded in R__seed_profile_test_data.sql — guaranteed to exist when profile service has run
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Inject
    AccountRepository accountRepository;

    @Inject
    TransactionRepository transactionRepository;

    private AccountResource accountResource;
    private TransactionResource transactionResource;

    @BeforeEach
    void setUp() {
        AccountService accountService = new AccountService(accountRepository, transactionRepository);
        TransactionService transactionService = new TransactionService(transactionRepository);
        accountResource = new AccountResource(accountService);
        transactionResource = new TransactionResource(transactionService);
    }

    @Test
    void createAccount_addManualTransactions_balanceReflectsOpeningPlusTransactions() {
        UUID accountId = createAccount("IT Balance Test Account", new BigDecimal("1000.00"));

        // CREDIT transaction of 500 on top of opening balance 1000
        CreateTransactionRequest credit = new CreateTransactionRequest();
        credit.txnDate = "2026-07-01";
        credit.amount = new BigDecimal("500.00");
        credit.txnType = "CREDIT";
        credit.description = "IT salary credit";
        Response creditResponse = transactionResource.createTransaction(accountId, SEED_PROFILE_ID, credit);
        assertEquals(201, creditResponse.getStatus());
        assertEquals("CREDIT", ((TransactionResponse) creditResponse.getEntity()).txnType);

        // DEBIT transaction of 200 -> expected current_balance 1000 + 500 - 200 = 1300
        CreateTransactionRequest debit = new CreateTransactionRequest();
        debit.txnDate = "2026-07-02";
        debit.amount = new BigDecimal("200.00");
        debit.txnType = "DEBIT";
        debit.description = "IT grocery debit";
        Response debitResponse = transactionResource.createTransaction(accountId, SEED_PROFILE_ID, debit);
        assertEquals(201, debitResponse.getStatus());

        AccountBalanceResponse balance = accountResource.getAccountBalance(accountId, SEED_PROFILE_ID);
        assertEquals(accountId, balance.accountId);
        assertEquals(0, new BigDecimal("1000.00").compareTo(balance.openingBalance));
        assertEquals(0, new BigDecimal("1300.00").compareTo(balance.currentBalance));
    }

    @Test
    void createAccount_noTransactions_balanceEqualsOpeningBalance() {
        UUID accountId = createAccount("IT No-Txn Account", new BigDecimal("250.00"));

        AccountBalanceResponse balance = accountResource.getAccountBalance(accountId, SEED_PROFILE_ID);
        assertEquals(0, new BigDecimal("250.00").compareTo(balance.currentBalance));
        assertEquals(0, new BigDecimal("250.00").compareTo(balance.openingBalance));
    }

    @Test
    void createTransaction_negativeAmount_throwsBadRequest() {
        UUID accountId = createAccount("IT Negative Amount Account", new BigDecimal("100.00"));

        CreateTransactionRequest negative = new CreateTransactionRequest();
        negative.txnDate = "2026-07-01";
        negative.amount = new BigDecimal("-50.00");
        negative.txnType = "DEBIT";

        assertThrows(BadRequestException.class,
                () -> transactionResource.createTransaction(accountId, SEED_PROFILE_ID, negative));
    }

    private UUID createAccount(String name, BigDecimal openingBalance) {
        CreateAccountRequest request = new CreateAccountRequest();
        request.accountName = name;
        request.accountType = "SAVINGS";
        request.institutionName = "IT Test Bank";
        request.openingBalance = openingBalance;

        Response response = accountResource.createAccount(SEED_PROFILE_ID, request);
        assertEquals(201, response.getStatus());
        AccountResponse created = (AccountResponse) response.getEntity();
        assertNotNull(created.accountId);
        return created.accountId;
    }
}
