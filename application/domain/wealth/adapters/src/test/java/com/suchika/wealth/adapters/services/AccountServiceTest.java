package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.AccountBalance;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.wealth.ports.input.UpdateAccountClassificationCommand;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private static final LocalDate FIXED_TXN_DATE = LocalDate.of(2026, Month.JUNE, 1);

    private AccountService service;
    private FakeAccountRepository repo;
    private FakeTransactionRepository txnRepo;

    @BeforeEach
    void setUp() {
        repo = new FakeAccountRepository();
        txnRepo = new FakeTransactionRepository();
        service = new AccountService(repo, txnRepo);
    }

    private static CreateAccountCommand cmd(String name, AccountType type, String institution,
                                            BigDecimal openingBalance, BigDecimal creditLimit,
                                            BigDecimal interestRate, BigDecimal emiAmount) {
        return new CreateAccountCommand(name, type, institution, null, openingBalance, creditLimit, interestRate, emiAmount);
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
        CreateAccountCommand command = cmd("  ", AccountType.SAVINGS, "HDFC Bank", null, null, null, null);
        assertThrows(BadRequestException.class, () -> service.createAccount(null, command));
    }

    @Test
    void createAccount_nullType_throwsBadRequest() {
        CreateAccountCommand command = cmd("HDFC Savings", null, "HDFC Bank", null, null, null, null);
        assertThrows(BadRequestException.class, () -> service.createAccount(null, command));
    }

    @Test
    void createAccount_blankInstitution_throwsBadRequest() {
        CreateAccountCommand command = cmd("HDFC Savings", AccountType.SAVINGS, " ", null, null, null, null);
        assertThrows(BadRequestException.class, () -> service.createAccount(null, command));
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
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getAccount(randomId));
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
        UUID accountId = account.getId();

        assertThrows(BadRequestException.class,
                () -> service.updateAccount(accountId, "  ", null, null, null, null, null));
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
        UUID accountId = account.getId();
        repo.markHasTransactions(accountId);

        assertThrows(ConflictException.class,
                () -> service.updateAccount(accountId, null, null, null, null, null, false));
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
        UUID accountId = account.getId();
        repo.markHasTransactions(accountId);

        assertThrows(ConflictException.class, () -> service.deactivateAccount(accountId));
    }

    @Test
    void deactivateAccount_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.deactivateAccount(randomId));
    }

    // ---- Bug 2 fix: getAccountBalance = opening_balance + SUM(CREDIT) - SUM(DEBIT) ----

    @Test
    void getAccountBalance_noTransactions_equalsOpeningBalance() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", new BigDecimal("1000.00"), null, null, null));

        AccountBalance balance = service.getAccountBalance(account.getId(), null);

        assertEquals(0, new BigDecimal("1000.00").compareTo(balance.currentBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.totalCredits()));
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.totalDebits()));
    }

    @Test
    void getAccountBalance_withCreditsAndDebits_sumsCorrectly() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", new BigDecimal("1000.00"), null, null, null));

        txnRepo.addCredit(account.getId(), new BigDecimal("5000.00"));
        txnRepo.addDebit(account.getId(), new BigDecimal("2000.00"));

        AccountBalance balance = service.getAccountBalance(account.getId(), null);

        // 1000 + 5000 - 2000 = 4000
        assertEquals(0, new BigDecimal("4000.00").compareTo(balance.currentBalance()));
    }

    @Test
    void getAccountBalance_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getAccountBalance(randomId, null));
    }

    // ---- Epic 8 Phase 1: account classification metadata write path ----

    @Test
    void updateAccountClassification_setsProvidedFields() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        Account updated = service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand("EMERGENCY_FUND", "LIQUID", "SAFETY_NET", null, null, null, null, null));

        assertEquals("EMERGENCY_FUND", updated.getMetadata().get("category"));
        assertEquals("LIQUID", updated.getMetadata().get("liquidity_tier"));
        assertEquals("SAFETY_NET", updated.getMetadata().get("purpose_tag"));
    }

    @Test
    void updateAccountClassification_partialFields_leavesOthersUnset() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        Account updated = service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand("INVESTMENT", null, null, null, null, null, null, null));

        assertEquals("INVESTMENT", updated.getMetadata().get("category"));
        assertNull(updated.getMetadata().get("liquidity_tier"));
        assertNull(updated.getMetadata().get("purpose_tag"));
    }

    @Test
    void updateAccountClassification_secondCall_mergesRatherThanOverwrites() {
        Account account = service.createAccount(null,
                cmd("HDFC Savings", AccountType.SAVINGS, "HDFC Bank", null, null, null, null));

        service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand("INVESTMENT", null, null, null, null, null, null, null));
        Account updated = service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand(null, "LIQUID", null, null, null, null, null, null));

        assertEquals("INVESTMENT", updated.getMetadata().get("category"));
        assertEquals("LIQUID", updated.getMetadata().get("liquidity_tier"));
    }

    @Test
    void updateAccountClassification_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        UpdateAccountClassificationCommand command =
                new UpdateAccountClassificationCommand("INVESTMENT", null, null, null, null, null, null, null);
        assertThrows(NotFoundException.class,
                () -> service.updateAccountClassification(randomId, command));
    }

    @Test
    void updateAccountClassification_jointOwners_storedCommaJoined() {
        Account account = service.createAccount(null,
                cmd("Kotak Joint", AccountType.SAVINGS, "Kotak Bank", null, null, null, null));
        String ownerA = UUID.randomUUID().toString();
        String ownerB = UUID.randomUUID().toString();

        Account updated = service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand(null, null, null, List.of(ownerA, ownerB), null, null, null, null));

        assertEquals(ownerA + "," + ownerB, updated.getMetadata().get("joint_owners"));
    }

    @Test
    void updateAccountClassification_loanFields_storedInMetadata() {
        Account account = service.createAccount(null,
                cmd("Home Loan", AccountType.HOME_LOAN, "SBI", new java.math.BigDecimal("5000000"), null, new java.math.BigDecimal("8.75"), new java.math.BigDecimal("45000")));

        Account updated = service.updateAccountClassification(account.getId(),
                new UpdateAccountClassificationCommand(null, null, null, null, "5000000", "2023-04-01", "240", "savings-uuid-123"));

        assertEquals("5000000", updated.getMetadata().get("original_principal"));
        assertEquals("2023-04-01", updated.getMetadata().get("loan_start_date"));
        assertEquals("240", updated.getMetadata().get("original_tenure_months"));
        assertEquals("savings-uuid-123", updated.getMetadata().get("linked_offset_account_id"));
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
                        .metadata(account.getMetadata())
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

    static class FakeTransactionRepository implements TransactionRepository {
        private final List<Transaction> store = new ArrayList<>();

        void addCredit(UUID accountId, BigDecimal amount) {
            store.add(Transaction.builder().accountId(accountId).amount(amount)
                    .txnType(TxnType.CREDIT).txnDate(FIXED_TXN_DATE).description("test credit").build());
        }

        void addDebit(UUID accountId, BigDecimal amount) {
            store.add(Transaction.builder().accountId(accountId).amount(amount)
                    .txnType(TxnType.DEBIT).txnDate(FIXED_TXN_DATE).description("test debit").build());
        }

        @Override
        public Transaction save(Transaction transaction) {
            store.add(transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findById(UUID id) {
            return store.stream().filter(t -> id.equals(t.getId())).findFirst();
        }

        @Override
        public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
            return store.stream().filter(t -> accountId.equals(t.getAccountId())).toList();
        }

        @Override
        public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType,
                                                   int page, int size) {
            return findByAccountId(accountId, profileId, from, to, txnType);
        }

        @Override
        public long countByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
            return findByAccountId(accountId, profileId, from, to, txnType).size();
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

        @Override
        public void deleteByUploadId(UUID uploadId) {
            store.removeIf(t -> uploadId.equals(t.getUploadId()));
        }
    }
}
