package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.output.GoalRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GoalPanacheRepository.
 * Requires a running local PostgreSQL (app_db) with household schema and test seed data.
 * Profile seed (R__seed_profile_test_data.sql) must have run before these tests.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestProfile(GoalPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class GoalPanacheRepositoryTest {

    /**
     * Activates the 'integration-test' Quarkus config profile which re-enables
     * the datasource. The default 'test' profile disables the DB for @InjectMock tests.
     */
    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }

    // Seeded by R__seed_profile_test_data.sql via profile domain migrations
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // A second distinct UUID for profile-isolation tests — has no rows seeded for it
    private static final UUID OTHER_PROFILE_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000099");

    @Inject
    GoalRepository repository;

    @Inject
    EntityManager em;

    // --- save + findById ---

    @Test
    void save_andFindById_roundTrip() {
        Goal saved = repository.save(goal("Emergency Fund", new BigDecimal("100000.00"),
                new BigDecimal("5000.00"), LocalDate.of(2027, Month.DECEMBER, 31), "Safety net"));

        assertNotNull(saved.getId());
        assertEquals(SEED_PROFILE_ID, saved.getProfileId());
        assertEquals("Emergency Fund", saved.getGoalName());
        assertEquals(0, new BigDecimal("100000.00").compareTo(saved.getTargetAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCurrentAmount()));
        assertEquals(GoalStatus.ACTIVE, saved.getStatus());
        assertEquals("Safety net", saved.getNotes());
        assertNotNull(saved.getCreatedAt());

        Optional<Goal> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Emergency Fund", found.get().getGoalName());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    // --- findByProfileId filters ---

    @Test
    void findByProfileId_noStatusFilter_returnsAllGoals() {
        repository.save(goal("Goal Active", new BigDecimal("50000.00"), null, null, null));
        repository.save(goalWithStatus("Goal Achieved", new BigDecimal("20000.00"), GoalStatus.ACHIEVED));
        repository.save(goalWithStatus("Goal Paused", new BigDecimal("30000.00"), GoalStatus.PAUSED));

        List<Goal> results = repository.findByProfileId(SEED_PROFILE_ID, null);

        assertTrue(results.size() >= 3);
        assertTrue(results.stream().anyMatch(g -> "Goal Active".equals(g.getGoalName())));
        assertTrue(results.stream().anyMatch(g -> "Goal Achieved".equals(g.getGoalName())));
        assertTrue(results.stream().anyMatch(g -> "Goal Paused".equals(g.getGoalName())));
    }

    @Test
    void findByProfileId_activeStatusFilter_returnsOnlyActive() {
        repository.save(goal("Active Goal 1", new BigDecimal("50000.00"), null, null, null));
        repository.save(goal("Active Goal 2", new BigDecimal("75000.00"), null, null, null));
        repository.save(goalWithStatus("Achieved Goal", new BigDecimal("20000.00"), GoalStatus.ACHIEVED));

        List<Goal> activeOnly = repository.findByProfileId(SEED_PROFILE_ID, GoalStatus.ACTIVE);

        assertTrue(activeOnly.stream().allMatch(g -> g.getStatus() == GoalStatus.ACTIVE));
        assertTrue(activeOnly.stream().anyMatch(g -> "Active Goal 1".equals(g.getGoalName())));
        assertTrue(activeOnly.stream().anyMatch(g -> "Active Goal 2".equals(g.getGoalName())));
        assertTrue(activeOnly.stream().noneMatch(g -> "Achieved Goal".equals(g.getGoalName())));
    }

    @Test
    void findByProfileId_achievedStatusFilter_returnsOnlyAchieved() {
        repository.save(goalWithStatus("Completed Vacation Fund", new BigDecimal("30000.00"), GoalStatus.ACHIEVED));
        repository.save(goal("Ongoing Home Fund", new BigDecimal("500000.00"), null, null, null));

        List<Goal> achievedOnly = repository.findByProfileId(SEED_PROFILE_ID, GoalStatus.ACHIEVED);

        assertTrue(achievedOnly.stream().allMatch(g -> g.getStatus() == GoalStatus.ACHIEVED));
        assertTrue(achievedOnly.stream().anyMatch(g -> "Completed Vacation Fund".equals(g.getGoalName())));
        assertTrue(achievedOnly.stream().noneMatch(g -> "Ongoing Home Fund".equals(g.getGoalName())));
    }

    @Test
    void findByProfileId_pausedStatusFilter_returnsOnlyPaused() {
        repository.save(goalWithStatus("Paused Car Fund", new BigDecimal("400000.00"), GoalStatus.PAUSED));
        repository.save(goal("Active Education Fund", new BigDecimal("200000.00"), null, null, null));

        List<Goal> pausedOnly = repository.findByProfileId(SEED_PROFILE_ID, GoalStatus.PAUSED);

        assertTrue(pausedOnly.stream().allMatch(g -> g.getStatus() == GoalStatus.PAUSED));
        assertTrue(pausedOnly.stream().anyMatch(g -> "Paused Car Fund".equals(g.getGoalName())));
        assertTrue(pausedOnly.stream().noneMatch(g -> "Active Education Fund".equals(g.getGoalName())));
    }

    @Test
    void findByProfileId_orderedByCreatedAtDesc() {
        // Three goals inserted sequentially — DB ordering is by createdAt DESC
        repository.save(goal("First Goal", new BigDecimal("10000.00"), null, null, null));
        repository.save(goal("Second Goal", new BigDecimal("20000.00"), null, null, null));
        repository.save(goal("Third Goal", new BigDecimal("30000.00"), null, null, null));

        List<Goal> results = repository.findByProfileId(SEED_PROFILE_ID, GoalStatus.ACTIVE);

        // All three active goals must be present
        assertTrue(results.stream().anyMatch(g -> "First Goal".equals(g.getGoalName())));
        assertTrue(results.stream().anyMatch(g -> "Second Goal".equals(g.getGoalName())));
        assertTrue(results.stream().anyMatch(g -> "Third Goal".equals(g.getGoalName())));
    }

    // --- profile-scoped isolation ---

    @Test
    void findByProfileId_doesNotLeakAcrossProfiles() {
        repository.save(goal("Profile A Goal", new BigDecimal("50000.00"), null, null, null));

        // OTHER_PROFILE_ID has no FK row in profile.profile — querying it returns empty
        List<Goal> otherResults = repository.findByProfileId(OTHER_PROFILE_ID, null);

        assertTrue(otherResults.isEmpty());
    }

    // ---- Q54 pagination pass ----
    // Note: unlike InventoryItem (purchaseDate) and CalendarEvent (startDate), Goal
    // ordering is by createdAt — a DB-generated now() value that Postgres resolves
    // per-transaction, not per-statement. Since @TestTransaction runs each test in one
    // transaction, rows inserted here can share an identical createdAt, so — matching
    // the pre-existing findByProfileId_orderedByCreatedAtDesc test's own approach —
    // these assertions verify page sizes and full-set coverage, not per-page ordering.

    @Test
    void findByProfileId_paginated_returnsRequestedPageSizes_withNoDuplicatesOrGaps() {
        UUID profileId = saveProfile("Pagination Page Member");
        for (int i = 1; i <= 5; i++) {
            repository.save(goalForProfile(profileId, "Page Goal " + i, new BigDecimal("10000.00")));
        }

        List<Goal> firstPage = repository.findByProfileId(profileId, null, 0, 2);
        List<Goal> secondPage = repository.findByProfileId(profileId, null, 1, 2);
        List<Goal> thirdPage = repository.findByProfileId(profileId, null, 2, 2);

        assertEquals(2, firstPage.size());
        assertEquals(2, secondPage.size());
        assertEquals(1, thirdPage.size());

        Set<UUID> allIds = new HashSet<>();
        firstPage.forEach(g -> allIds.add(g.getId()));
        secondPage.forEach(g -> allIds.add(g.getId()));
        thirdPage.forEach(g -> allIds.add(g.getId()));
        assertEquals(5, allIds.size(), "Expected 5 distinct goals across all pages with no duplicates or gaps");
    }

    @Test
    void countByProfileId_returnsTotalMatchingFilterIgnoringPage() {
        UUID profileId = saveProfile("Pagination Count Member");
        for (int i = 1; i <= 5; i++) {
            repository.save(goalForProfile(profileId, "Counted Goal " + i, new BigDecimal("10000.00")));
        }

        long total = repository.countByProfileId(profileId, null);

        assertEquals(5, total);
    }

    @Test
    void countByProfileId_appliesSameFiltersAsFindByProfileId() {
        UUID profileId = saveProfile("Pagination Filter Member");
        repository.save(goalForProfile(profileId, "Active Filter Goal", new BigDecimal("10000.00")));
        repository.save(goalWithStatusForProfile(profileId, "Paused Filter Goal", new BigDecimal("20000.00"), GoalStatus.PAUSED));

        assertEquals(1, repository.countByProfileId(profileId, GoalStatus.ACTIVE));
    }

    // --- currentAmount update (used by projection engine) ---

    @Test
    void save_updateCurrentAmount_persistsChange() {
        Goal saved = repository.save(goal("Savings Goal", new BigDecimal("100000.00"),
                new BigDecimal("5000.00"), null, null));

        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCurrentAmount()));

        Goal withProgress = Goal.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .goalName(saved.getGoalName())
                .targetAmount(saved.getTargetAmount())
                .currentAmount(new BigDecimal("25000.00"))
                .monthlySaving(saved.getMonthlySaving())
                .status(GoalStatus.ACTIVE)
                .createdAt(saved.getCreatedAt())
                .build();

        Goal result = repository.save(withProgress);

        assertEquals(0, new BigDecimal("25000.00").compareTo(result.getCurrentAmount()));
    }

    @Test
    void save_statusTransition_activeToPaused_persistsChange() {
        Goal saved = repository.save(goal("Transition Goal", new BigDecimal("50000.00"), null, null, null));
        assertEquals(GoalStatus.ACTIVE, saved.getStatus());

        Goal paused = Goal.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .goalName(saved.getGoalName())
                .targetAmount(saved.getTargetAmount())
                .currentAmount(saved.getCurrentAmount())
                .status(GoalStatus.PAUSED)
                .createdAt(saved.getCreatedAt())
                .build();

        Goal result = repository.save(paused);

        assertEquals(GoalStatus.PAUSED, result.getStatus());
    }

    // --- delete ---

    @Test
    void deleteById_removesGoal() {
        Goal saved = repository.save(goal("Temporary Goal", new BigDecimal("10000.00"), null, null, null));
        UUID id = saved.getId();
        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        assertTrue(repository.findById(id).isEmpty());
    }

    // --- existsById ---

    @Test
    void existsById_falseForUnknownId() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void existsById_trueAfterSave() {
        Goal saved = repository.save(goal("Exists Check Goal", new BigDecimal("15000.00"), null, null, null));
        assertTrue(repository.existsById(saved.getId()));
    }



    // --- computed properties from domain entity ---

    @Test
    void progressPercent_computedCorrectly_afterCurrentAmountUpdate() {
        Goal saved = repository.save(goal("Progress Goal", new BigDecimal("100000.00"),
                new BigDecimal("5000.00"), null, null));

        Goal withProgress = Goal.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .goalName(saved.getGoalName())
                .targetAmount(saved.getTargetAmount())
                .currentAmount(new BigDecimal("30000.00"))
                .monthlySaving(saved.getMonthlySaving())
                .status(GoalStatus.ACTIVE)
                .createdAt(saved.getCreatedAt())
                .build();

        Goal updated = repository.save(withProgress);
        Optional<Goal> reloaded = repository.findById(updated.getId());

        assertTrue(reloaded.isPresent());
        // progressPercent = 30000 / 100000 * 100 = 30.00
        assertEquals(0, new BigDecimal("30.00").compareTo(reloaded.get().progressPercent()));
    }

    @Test
    void daysToCompletion_presentWhenMonthlySavingSet() {
        Goal saved = repository.save(goal("Days Estimate Goal", new BigDecimal("30000.00"),
                new BigDecimal("3000.00"), null, null));

        Optional<Goal> reloaded = repository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        // Remaining = 30000; daily rate = 3000/30 = 100; days = ceil(30000/100) = 300
        assertTrue(reloaded.get().daysToCompletion().isPresent());
        assertEquals(300L, reloaded.get().daysToCompletion().getAsLong());
    }

    // ---- helpers ----

    private Goal goal(String name, BigDecimal targetAmount, BigDecimal monthlySaving,
                      LocalDate targetDate, String notes) {
        return Goal.builder()
                .profileId(SEED_PROFILE_ID)
                .goalName(name)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .monthlySaving(monthlySaving)
                .targetDate(targetDate)
                .status(GoalStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    private Goal goalWithStatus(String name, BigDecimal targetAmount, GoalStatus status) {
        return Goal.builder()
                .profileId(SEED_PROFILE_ID)
                .goalName(name)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .status(status)
                .build();
    }

    private Goal goalForProfile(UUID profileId, String name, BigDecimal targetAmount) {
        return Goal.builder()
                .profileId(profileId)
                .goalName(name)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .status(GoalStatus.ACTIVE)
                .build();
    }

    private Goal goalWithStatusForProfile(UUID profileId, String name, BigDecimal targetAmount, GoalStatus status) {
        return Goal.builder()
                .profileId(profileId)
                .goalName(name)
                .targetAmount(targetAmount)
                .currentAmount(BigDecimal.ZERO)
                .status(status)
                .build();
    }

    /**
     * Inserts a minimal profile.admin + profile.profile row pair via native SQL so a
     * fresh, test-scoped profile_id (FK to profile.profile) is available for pagination
     * tests — avoids count assertions being polluted by seed data or other tests sharing
     * SEED_PROFILE_ID. Rolled back automatically by @TestTransaction.
     */
    private UUID saveProfile(String fullName) {
        UUID adminId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.admin (id, display_name) VALUES (?1, ?2)")
                .setParameter(1, adminId)
                .setParameter(2, "Test Admin")
                .executeUpdate();

        UUID profileId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin) "
                        + "VALUES (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, profileId)
                .setParameter(2, adminId)
                .setParameter(3, fullName)
                .setParameter(4, LocalDate.of(1990, Month.JANUARY, 1))
                .setParameter(5, "OTHER")
                .executeUpdate();

        return profileId;
    }
}
