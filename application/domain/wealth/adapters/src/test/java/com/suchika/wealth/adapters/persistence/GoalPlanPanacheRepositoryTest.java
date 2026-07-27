package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;
import com.suchika.wealth.ports.output.GoalPlanRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for GoalPlanPanacheRepository (ADR-022 Phase 1).
 * Requires a running local PostgreSQL (app_db). Each test runs in a transaction
 * that is rolled back on completion. Mirrors PhysicalAssetPanacheRepositoryTest's
 * saveProfile()-style helpers, but goal_plan is admin_id-scoped (not profile_id).
 */
@QuarkusTest
@TestProfile(GoalPlanPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class GoalPlanPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    @Inject
    GoalPlanRepository repository;

    @Inject
    EntityManager em;

    @Test
    void save_andFindById_roundTrip() {
        UUID adminId = saveAdmin();

        GoalPlan saved = repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null,
                "Reach a state where MF corpus exceeds outstanding debt", null, null, null, null, null)));

        assertNotNull(saved.getId());
        assertEquals(adminId, saved.getAdminId());
        assertEquals(GoalType.DEBT_CROSSOVER, saved.getGoalType());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<GoalPlan> found = repository.findById(saved.getId(), adminId);
        assertTrue(found.isPresent());
        assertEquals("Reach a state where MF corpus exceeds outstanding debt", found.get().getObjective());
    }

    @Test
    void findById_wrongAdmin_returnsEmpty() {
        UUID ownerAdminId = saveAdmin();
        UUID otherAdminId = saveAdmin();
        GoalPlan saved = repository.save(GoalPlan.create(ownerAdminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null,
                "Objective", null, null, null, null, null)));

        Optional<GoalPlan> found = repository.findById(saved.getId(), otherAdminId);

        assertTrue(found.isEmpty(), "A different admin_id must not be able to fetch another household's goal plan");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void findAll_scopedByAdmin_excludesOtherHouseholds() {
        UUID ownerAdminId = saveAdmin();
        UUID otherAdminId = saveAdmin();
        repository.save(GoalPlan.create(ownerAdminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Objective 1", null, null, null, null, null)));
        repository.save(GoalPlan.create(ownerAdminId, new GoalPlan.Spec(GoalType.INSURANCE_FREE, null, "Objective 2", null, null, null, null, null)));
        repository.save(GoalPlan.create(otherAdminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Other household's plan", null, null, null, null, null)));

        List<GoalPlan> ownerPlans = repository.findAll(ownerAdminId);

        assertEquals(2, ownerPlans.size());
    }

    @Test
    void yearOne_multipleChildren_eachGetsOwnRow() {
        UUID adminId = saveAdmin();
        UUID child1 = saveChildProfile(adminId, "Aanya");
        UUID child2 = saveChildProfile(adminId, "Zoya");

        repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.YEAR_ONE, child1, "Fund Aanya's year one", null, null,
                new BigDecimal("1000000"), new BigDecimal("0.06"), 10)));
        repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.YEAR_ONE, child2, "Fund Zoya's year one", null, null,
                new BigDecimal("1500000"), new BigDecimal("0.06"), 12)));

        List<GoalPlan> plans = repository.findAll(adminId);

        assertEquals(2, plans.size());
        assertTrue(plans.stream().allMatch(p -> p.getGoalType() == GoalType.YEAR_ONE));
    }

    @Test
    void existsByAdminGoalTypeBeneficiary_singletonType_detectsDuplicate() {
        UUID adminId = saveAdmin();
        repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null, "Objective", null, null, null, null, null)));

        assertTrue(repository.existsByAdminGoalTypeBeneficiary(adminId, GoalType.DEBT_CROSSOVER, null));
        assertFalse(repository.existsByAdminGoalTypeBeneficiary(adminId, GoalType.FREEDOM_RUNWAY, null));
    }

    @Test
    void existsByAdminGoalTypeBeneficiary_yearOne_differentiatesPerChild() {
        UUID adminId = saveAdmin();
        UUID child1 = saveChildProfile(adminId, "Aanya");
        UUID child2 = saveChildProfile(adminId, "Zoya");
        repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.YEAR_ONE, child1, "Fund Aanya's year one", null, null,
                new BigDecimal("1000000"), new BigDecimal("0.06"), 10)));

        assertTrue(repository.existsByAdminGoalTypeBeneficiary(adminId, GoalType.YEAR_ONE, child1));
        assertFalse(repository.existsByAdminGoalTypeBeneficiary(adminId, GoalType.YEAR_ONE, child2));
    }

    @Test
    void uniqueConstraint_duplicateSingletonGoalType_violatesNullsNotDistinct() {
        UUID adminId = saveAdmin();
        repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null, "First", null, null, null, null, null)));
        GoalPlan duplicate = GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null, "Second (duplicate)", null, null, null, null, null));

        repository.save(duplicate);
        assertThrows(jakarta.persistence.PersistenceException.class, em::flush,
                "UNIQUE NULLS NOT DISTINCT must reject a second DEBT_CROSSOVER row with the same NULL beneficiary_profile_id");
    }

    @Test
    void replaceMilestones_bulkReplace_returnsOrderedList() {
        UUID adminId = saveAdmin();
        GoalPlan plan = repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Objective", null, null, null, null, null)));

        List<GoalMilestone> milestones = List.of(
                GoalMilestone.create(null, 0, "First", new BigDecimal("25"), false, false, "Quarter way"),
                GoalMilestone.create(null, 1, "Second", new BigDecimal("50"), false, false, "Halfway"));

        List<GoalMilestone> saved = repository.replaceMilestones(plan.getId(), milestones);

        assertEquals(2, saved.size());
        assertEquals("First", saved.get(0).getLabel());
        assertEquals("Second", saved.get(1).getLabel());

        List<GoalMilestone> found = repository.findMilestones(plan.getId());
        assertEquals(2, found.size());
    }

    @Test
    void replaceMilestones_secondCall_fullyReplacesFirst() {
        UUID adminId = saveAdmin();
        GoalPlan plan = repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Objective", null, null, null, null, null)));

        repository.replaceMilestones(plan.getId(), List.of(
                GoalMilestone.create(null, 0, "Old milestone", new BigDecimal("10"), false, false, "Old")));
        repository.replaceMilestones(plan.getId(), List.of(
                GoalMilestone.create(null, 0, "New milestone", new BigDecimal("20"), false, false, "New")));

        List<GoalMilestone> found = repository.findMilestones(plan.getId());

        assertEquals(1, found.size());
        assertEquals("New milestone", found.get(0).getLabel());
    }

    @Test
    void saveMilestone_singleMilestoneToggle_persistsAchievedFlag() {
        UUID adminId = saveAdmin();
        GoalPlan plan = repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Objective", null, null, null, null, null)));
        repository.replaceMilestones(plan.getId(), List.of(
                GoalMilestone.create(null, 0, "Checklist item", null, true, false, "Manual step")));
        GoalMilestone milestone = repository.findMilestones(plan.getId()).get(0);

        milestone.setAchieved(true);
        repository.saveMilestone(plan.getId(), milestone);

        Optional<GoalMilestone> found = repository.findMilestoneById(plan.getId(), milestone.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isAchieved());
    }

    @Test
    void replaceRules_and_replaceTriggerEvents_bulkReplace() {
        UUID adminId = saveAdmin();
        GoalPlan plan = repository.save(GoalPlan.create(adminId, new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, null, "Objective", null, null, null, null, null)));

        List<GoalRule> rules = repository.replaceRules(plan.getId(), List.of(
                GoalRule.create(null, 0, "No Liquidation", "Never liquidate mid-goal")));
        List<GoalTriggerEvent> events = repository.replaceTriggerEvents(plan.getId(), List.of(
                GoalTriggerEvent.create(null, 0, "Bonus received", "Bonus exceeds 1 lakh", "Allocate 50% to MF")));

        assertEquals(1, rules.size());
        assertEquals("No Liquidation", rules.get(0).getRuleName());
        assertEquals(1, events.size());
        assertEquals("Bonus received", events.get(0).getEventName());
        assertEquals(1, repository.findRules(plan.getId()).size());
        assertEquals(1, repository.findTriggerEvents(plan.getId()).size());
    }

    // ---- Helpers ----

    private UUID saveAdmin() {
        UUID adminId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.admin (id, display_name) VALUES (?1, ?2)")
                .setParameter(1, adminId)
                .setParameter(2, "Test Admin")
                .executeUpdate();
        return adminId;
    }

    private UUID saveChildProfile(UUID adminId, String fullName) {
        UUID profileId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin) "
                        + "VALUES (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, profileId)
                .setParameter(2, adminId)
                .setParameter(3, fullName)
                .setParameter(4, LocalDate.of(2015, Month.JANUARY, 1))
                .setParameter(5, "CHILD")
                .executeUpdate();
        return profileId;
    }
}
