package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.wealth.ports.output.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService service;
    private FakeAccountRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FakeAccountRepository();
        service = new AccountService(repo);
    }

    private static CreateAccountCommand cmd(String name, AccountType type, String institution,
                                            BigDecimal openingBalance, BigDecimal creditLimit,
                                            BigDecimal interestRate, BigDecimal emiAmount) {
        return new CreateAccountCommand(name, type, institution, openingBalance, creditLimit, interestRate, emiAmount);
    }

    @Test
    void createAccount_happyPath_returnsAccountWithId() {
        Account result = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", new BigDecimal("50000.00"), null, null, null));

        assertNotNull(result.getId());
        assertEquals("HDFC Savings", result.getAccountName());
        assertEquals(AccountType.SAVINGS, result.getAccountType());
        assertEquals("HDFC Bank", result.getInstitutionName());
        assertTrue(result.isActive());
    }

    @Test
    void createAccount_nullOpeningBalance_defaultsToZero() {
        Account result = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getOpeningBalance()));
    }

    @Test
    void createAccount_blankName_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.createAccount(null,
                        cmd("  ", AccountType.SAVINGS, "HDFC Bank", null, null, null, null)));
    }

    @Test
    void createAccount_nullType_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.createAccount(null,
                        cmd("HDFC Savings", null, "HDFC Bank", null, null, null, null)));
    }

    @Test
    void createAccount_blankInstitution_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.createAccount(null,
                        cmd("HDFC Savings", AccountType.SAVINGS, " ", null, null, null, null)));
    }

    @Test
    void getAccount_found_returnsAccount() {
        Account created = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        Account found = service.getAccount(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getAccount_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.getAccount(UUID.randomUUID()));
    }

    @Test
    void listAccounts_filtersByTypeAndActive() {
        service.createAccount(null, cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));
        service.createAccount(null, cmd("ICICI Credit Card", AccountType.CREDIT_CARD, "ICICI Bank", null, null, null, null));

        assertEquals(1, service.listAccounts(null, AccountType.CREDIT_CARD, null).size());
        assertEquals(2, service.listAccounts(null, null, true).size());
    }

    @Test
    void updateAccount_partialFields_updatesOnlyProvided() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", new BigDecimal("1000"), null, null, null));

        Account updated = service.updateAccount(account.getId(), "HDFC Salary", null, null, null, null, null);

        assertEquals("HDFC Salary", updated.getAccountName());
        assertEquals(0, new BigDecimal("1000").compareTo(updated.getOpeningBalance()));
    }

    @Test
    void updateAccount_blankName_throwsBadRequest() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        assertThrows(BadRequestException.class,
                () -> service.updateAccount(account.getId(), "  ", null, null, null, null, null));
    }

    @Test
    void updateAccount_deactivateWithoutTransactions_succeeds() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        Account updated = service.updateAccount(account.getId(), null, null, null, null, null, false);

        assertFalse(updated.isActive());
    }

    @Test
    void updateAccount_deactivateWithTransactions_throwsConflict() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));
        repo.markHasTransactions(account.getId());

        assertThrows(ConflictException.class,
                () -> service.updateAccount(account.getId(), null, null, null, null, null, false));
    }

    @Test
    void deactivateAccount_noTransactions_setsInactive() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        service.deactivateAccount(account.getId());

        assertFalse(service.getAccount(account.getId()).isActive());
    }

    @Test
    void deactivateAccount_withTransactions_throwsConflict() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));
        repo.markHasTransactions(account.getId());

        assertThrows(ConflictException.class, () -> service.deactivateAccount(account.getId()));
    }

    @Test
    void deactivateAccount_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.deactivateAccount(UUID.randomUUID()));
    }

    // ---- Fake repository ----

    static class FakeAccountRepository implements AccountRepository {
        private final Map<UUID, Account> store = new HashMap<>();
        private final Set<UUID> withTransactions = new HashSet<>();

        void markHasTransactions(UUID accountId) {
            withTransactions.add(accountId);
        }

        @Override
        public Account save(Account account) {
            if (account.getId() == null) {
                account = Account.builder()
                        .id(UUID.randomUUID())
                        .profileId(account.getProfileId())
                        .accountName(account.getAccountName())
                        .accountType(account.getAccountType())
                        .institutionName(account.getInstitutionName())
                        .currency(account.getCurrency())
                        .openingBalance(account.getOpeningBalance())
                        .creditLimit(account.getCreditLimit())
                        .interestRate(account.getInterestRate())
                        .emiAmount(account.getEmiAmount())
                        .active(account.isActive())
                        .build();
            }
            store.put(account.getId(), account);
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Account> findAll(UUID profileId, AccountType accountType, Boolean isActive) {
            return store.values().stream()
                    .filter(a -> profileId == null || profileId.equals(a.getProfileId()))
                    .filter(a -> accountType == null || accountType == a.getAccountType())
                    .filter(a -> isActive == null || isActive.equals(a.isActive()))
                    .toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }

        @Override
        public boolean hasTransactions(UUID accountId) {
            return withTransactions.contains(accountId);
        }
    }
}
