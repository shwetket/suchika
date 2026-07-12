package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.output.AccountRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AccountPanacheRepository.
 * Requires a running local PostgreSQL (app_db).
 * profile_id is set to null (nullable column) to avoid profile FK dependency.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestProfile(AccountPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class AccountPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    @Inject
    AccountRepository repository;

    @Test
    void save_andFindById_roundTrip() {
        Account saved = repository.save(account("HDFC Savings", AccountType.SAVINGS, "HDFC Bank",
                new BigDecimal("50000.00")));

        assertNotNull(saved.getId());
        assertEquals("HDFC Savings", saved.getAccountName());
        assertEquals(AccountType.SAVINGS, saved.getAccountType());
        assertEquals("HDFC Bank", saved.getInstitutionName());
        assertEquals(0, new BigDecimal("50000.00").compareTo(saved.getOpeningBalance()));
        assertTrue(saved.isActive());

        Optional<Account> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("HDFC Savings", found.get().getAccountName());
    }

    @Test
    void save_andFindById_roundTripsBalanceAsOf() {
        java.time.LocalDate asOf = java.time.LocalDate.of(2026, java.time.Month.JULY, 10);
        Account toSave = Account.builder()
                .accountName("Kotak Savings")
                .accountType(AccountType.SAVINGS)
                .institutionName("Kotak Mahindra Bank")
                .openingBalance(new BigDecimal("11.00"))
                .balanceAsOf(asOf)
                .build();

        Account saved = repository.save(toSave);

        Optional<Account> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(asOf, found.get().getBalanceAsOf());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    // ---- v0.5.1 remediation (Tier B): profile-scoped findById ----

    @Test
    void findByIdProfileScoped_ownerProfile_returnsAccount() {
        UUID seededProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Account saved = repository.save(accountWithProfile("Scoped Find Acc", AccountType.SAVINGS, "Bank A", seededProfileId));

        Optional<Account> found = repository.findById(saved.getId(), seededProfileId);

        assertTrue(found.isPresent());
        assertEquals("Scoped Find Acc", found.get().getAccountName());
    }

    @Test
    void findByIdProfileScoped_wrongProfile_returnsEmpty() {
        UUID seededProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID otherProfileId = UUID.randomUUID();
        Account saved = repository.save(accountWithProfile("Cross Profile Acc", AccountType.SAVINGS, "Bank A", seededProfileId));

        Optional<Account> found = repository.findById(saved.getId(), otherProfileId);

        assertTrue(found.isEmpty(), "A different profile_id must not be able to fetch another profile's account");
    }

    @Test
    void findByIdProfileScoped_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void findAll_filtersByAccountType() {
        repository.save(account("Savings Acc", AccountType.SAVINGS, "HDFC Bank", null));
        repository.save(account("CC Acc", AccountType.CREDIT_CARD, "ICICI Bank", null));
        repository.save(account("Loan Acc", AccountType.HOME_LOAN, "SBI", null));

        List<Account> savingsOnly = repository.findAll(null, AccountType.SAVINGS, null);
        assertTrue(savingsOnly.stream().allMatch(a -> a.getAccountType() == AccountType.SAVINGS));
        assertTrue(savingsOnly.stream().anyMatch(a -> a.getAccountName().equals("Savings Acc")));

        List<Account> ccOnly = repository.findAll(null, AccountType.CREDIT_CARD, null);
        assertTrue(ccOnly.stream().allMatch(a -> a.getAccountType() == AccountType.CREDIT_CARD));
    }

    @Test
    void findAll_filtersByActive() {
        repository.save(account("Active Acc", AccountType.SAVINGS, "Bank A", null));
        Account inactive = repository.save(account("Inactive Acc", AccountType.SAVINGS, "Bank B", null));

        // Deactivate the second account
        inactive.setActive(false);
        repository.save(inactive);

        List<Account> activeOnly = repository.findAll(null, null, true);
        assertTrue(activeOnly.stream().anyMatch(a -> a.getAccountName().equals("Active Acc")));
        assertTrue(activeOnly.stream().noneMatch(a -> a.getAccountName().equals("Inactive Acc")));

        List<Account> inactiveOnly = repository.findAll(null, null, false);
        assertTrue(inactiveOnly.stream().anyMatch(a -> a.getAccountName().equals("Inactive Acc")));
    }

    @Test
    void findAll_combinedFilter_typeAndActive() {
        repository.save(account("Savings A", AccountType.SAVINGS, "Bank A", null));
        Account savingsB = repository.save(account("Savings B", AccountType.SAVINGS, "Bank B", null));
        repository.save(account("CC", AccountType.CREDIT_CARD, "Bank C", null));

        savingsB.setActive(false);
        repository.save(savingsB);

        List<Account> activeSavings = repository.findAll(null, AccountType.SAVINGS, true);
        assertTrue(activeSavings.stream().anyMatch(a -> a.getAccountName().equals("Savings A")));
        assertTrue(activeSavings.stream().noneMatch(a -> a.getAccountName().equals("Savings B")));
        assertTrue(activeSavings.stream().noneMatch(a -> a.getAccountType() == AccountType.CREDIT_CARD));
    }

    @Test
    void findAll_filtersByProfileId() {
        // Use the seeded profile ID — random UUIDs would violate the FK on profile_id
        UUID seededProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        repository.save(accountWithProfile("Profile Scoped Acc 1", AccountType.SAVINGS, "Bank A", seededProfileId));
        repository.save(accountWithProfile("Profile Scoped Acc 2", AccountType.CURRENT, "Bank A", seededProfileId));
        // null profile_id — should not be returned when filtering by seededProfileId
        repository.save(account("Unscoped Acc", AccountType.SAVINGS, "Bank B", null));

        List<Account> forProfile = repository.findAll(seededProfileId, null, null);
        assertTrue(forProfile.stream().allMatch(a -> seededProfileId.equals(a.getProfileId())));
        assertTrue(forProfile.stream().anyMatch(a -> a.getAccountName().equals("Profile Scoped Acc 1")));
        assertTrue(forProfile.stream().anyMatch(a -> a.getAccountName().equals("Profile Scoped Acc 2")));
        assertTrue(forProfile.stream().noneMatch(a -> a.getAccountName().equals("Unscoped Acc")));

        List<Account> unfiltered = repository.findAll(null, null, null);
        assertTrue(unfiltered.stream().anyMatch(a -> a.getAccountName().equals("Unscoped Acc")));
    }

    @Test
    void existsById_returnsCorrectly() {
        Account saved = repository.save(account("Exists Test", AccountType.SAVINGS, "Bank", null));

        assertTrue(repository.existsById(saved.getId()));
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void hasTransactions_falseForNewAccount() {
        Account saved = repository.save(account("No Txn Acc", AccountType.SAVINGS, "Bank", null));

        assertFalse(repository.hasTransactions(saved.getId()));
    }

    @Test
    void save_update_persistsChanges() {
        Account saved = repository.save(account("Original Name", AccountType.SAVINGS, "Bank",
                new BigDecimal("1000.00")));

        saved.setAccountName("Updated Name");
        saved.setOpeningBalance(new BigDecimal("2000.00"));
        Account updated = repository.save(saved);

        assertEquals("Updated Name", updated.getAccountName());
        assertEquals(0, new BigDecimal("2000.00").compareTo(updated.getOpeningBalance()));
    }

    private Account account(String name, AccountType type, String institution, BigDecimal openingBalance) {
        return Account.builder()
                .accountName(name)
                .accountType(type)
                .institutionName(institution)
                .openingBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO)
                .build();
    }

    private Account accountWithProfile(String name, AccountType type, String institution, UUID profileId) {
        return Account.builder()
                .accountName(name)
                .accountType(type)
                .institutionName(institution)
                .openingBalance(BigDecimal.ZERO)
                .profileId(profileId)
                .build();
    }
}
