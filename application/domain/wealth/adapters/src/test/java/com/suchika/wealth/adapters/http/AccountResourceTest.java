package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.AccountResponse;
import com.suchika.wealth.adapters.http.dto.CreateAccountRequest;
import com.suchika.wealth.adapters.http.dto.ListAccountsResponse;
import com.suchika.wealth.adapters.http.dto.UpdateAccountClassificationRequest;
import com.suchika.wealth.adapters.http.dto.UpdateAccountRequest;
import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.domain.AmortizationSummary;
import com.suchika.wealth.ports.input.AccountBalance;
import com.suchika.wealth.ports.input.AccountUseCase;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.wealth.ports.input.UpdateAccountClassificationCommand;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountResourceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private AccountResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new AccountResource(useCase);
    }

    @Test
    void listAccounts_returns200_withAccountList() {
        useCase.accountsToReturn = List.of(buildAccount());

        Response response = resource.listAccounts(null, true, PROFILE_ID);

        assertEquals(200, response.getStatus());
        ListAccountsResponse body = (ListAccountsResponse) response.getEntity();
        assertEquals(1, body.accounts.size());
        assertEquals("SBI Savings", body.accounts.get(0).accountName);
    }

    @Test
    void listAccounts_invalidAccountType_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listAccounts("NOT_A_TYPE", true, PROFILE_ID));
    }

    @Test
    void createAccount_returns201_withCreatedAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.accountName = "HDFC Salary";
        request.accountType = "SAVINGS";
        request.openingBalance = BigDecimal.valueOf(5000);
        useCase.accountToReturn = buildAccount();

        Response response = resource.createAccount(PROFILE_ID, request);

        assertEquals(201, response.getStatus());
        assertEquals(PROFILE_ID, useCase.lastCreateProfileId);
        assertEquals(AccountType.SAVINGS, useCase.lastCreateCommand.accountType());
        AccountResponse body = (AccountResponse) response.getEntity();
        assertEquals("SBI Savings", body.accountName);
    }

    @Test
    void createAccount_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.createAccount(PROFILE_ID, null));
    }

    @Test
    void createAccount_missingAccountType_throwsBadRequest() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.accountName = "No Type";
        assertThrows(BadRequestException.class, () -> resource.createAccount(PROFILE_ID, request));
    }

    @Test
    void getAccount_returnsAccountResponse() {
        useCase.accountToReturn = buildAccount();

        AccountResponse response = resource.getAccount(ACCOUNT_ID);

        assertEquals("SBI Savings", response.accountName);
        assertEquals(ACCOUNT_ID, useCase.lastGetAccountId);
    }

    @Test
    void updateAccount_returnsUpdatedAccount() {
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.accountName = "Renamed";
        useCase.accountToReturn = buildAccount();

        AccountResponse response = resource.updateAccount(ACCOUNT_ID, request);

        assertEquals("SBI Savings", response.accountName);
        assertEquals(ACCOUNT_ID, useCase.lastUpdateAccountId);
        assertEquals("Renamed", useCase.lastUpdateAccountName);
    }

    @Test
    void updateAccount_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateAccount(ACCOUNT_ID, null));
    }

    @Test
    void deactivateAccount_returns204() {
        Response response = resource.deactivateAccount(ACCOUNT_ID);

        assertEquals(204, response.getStatus());
        assertEquals(ACCOUNT_ID, useCase.lastDeactivateAccountId);
    }

    @Test
    void getAccountBalance_returnsBalanceResponse() {
        useCase.balanceToReturn = new AccountBalance(
                ACCOUNT_ID, BigDecimal.valueOf(1000), BigDecimal.valueOf(500), BigDecimal.valueOf(200),
                BigDecimal.valueOf(1300));

        var response = resource.getAccountBalance(ACCOUNT_ID, PROFILE_ID);

        assertEquals(BigDecimal.valueOf(1300), response.currentBalance);
    }

    @Test
    void updateAccountClassification_returnsUpdatedAccount() {
        UpdateAccountClassificationRequest request = new UpdateAccountClassificationRequest();
        request.category = "HOUSEHOLD_CORE";
        useCase.accountToReturn = buildAccount();

        AccountResponse response = resource.updateAccountClassification(ACCOUNT_ID, request);

        assertEquals("SBI Savings", response.accountName);
        assertEquals(ACCOUNT_ID, useCase.lastClassificationAccountId);
        assertEquals("HOUSEHOLD_CORE", useCase.lastClassificationCommand.category());
    }

    @Test
    void updateAccountClassification_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateAccountClassification(ACCOUNT_ID, null));
    }

    @Test
    void getAmortization_returnsAmortizationResponse() {
        useCase.amortizationToReturn = AmortizationSummary.builder()
                .monthlyEmi(BigDecimal.valueOf(15000))
                .outstandingBalance(BigDecimal.valueOf(500000))
                .remainingMonths(36)
                .totalInterestRemaining(BigDecimal.valueOf(50000))
                .principalPaid(BigDecimal.valueOf(100000))
                .interestPaid(BigDecimal.valueOf(20000))
                .build();

        var response = resource.getAmortization(ACCOUNT_ID, PROFILE_ID);

        assertEquals(36, response.remainingMonths);
        assertTrue(response.monthlyEmi.compareTo(BigDecimal.valueOf(15000)) == 0);
    }

    private Account buildAccount() {
        return Account.builder()
                .id(ACCOUNT_ID)
                .profileId(PROFILE_ID)
                .accountName("SBI Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("SBI")
                .openingBalance(BigDecimal.valueOf(1000))
                .active(true)
                .build();
    }

    static class StubUseCase implements AccountUseCase {
        List<Account> accountsToReturn = List.of();
        Account accountToReturn;
        AccountBalance balanceToReturn;
        AmortizationSummary amortizationToReturn;

        UUID lastCreateProfileId;
        CreateAccountCommand lastCreateCommand;
        UUID lastGetAccountId;
        UUID lastUpdateAccountId;
        String lastUpdateAccountName;
        UUID lastDeactivateAccountId;
        UUID lastClassificationAccountId;
        UpdateAccountClassificationCommand lastClassificationCommand;

        @Override
        public Account createAccount(UUID profileId, CreateAccountCommand command) {
            lastCreateProfileId = profileId;
            lastCreateCommand = command;
            return accountToReturn;
        }

        @Override
        public Account getAccount(UUID id) {
            lastGetAccountId = id;
            return accountToReturn;
        }

        @Override
        public List<Account> listAccounts(UUID profileId, AccountType accountType, Boolean isActive) {
            return accountsToReturn;
        }

        @Override
        public Account updateAccount(UUID id, String accountName, BigDecimal openingBalance, BigDecimal creditLimit,
                                      BigDecimal interestRate, BigDecimal emiAmount, Boolean isActive) {
            lastUpdateAccountId = id;
            lastUpdateAccountName = accountName;
            return accountToReturn;
        }

        @Override
        public void deactivateAccount(UUID id) {
            lastDeactivateAccountId = id;
        }

        @Override
        public AccountBalance getAccountBalance(UUID accountId, UUID profileId) {
            return balanceToReturn;
        }

        @Override
        public Account updateAccountClassification(UUID id, UpdateAccountClassificationCommand command) {
            lastClassificationAccountId = id;
            lastClassificationCommand = command;
            return accountToReturn;
        }

        @Override
        public AmortizationSummary getAmortization(UUID accountId, UUID profileId) {
            return amortizationToReturn;
        }
    }
}
