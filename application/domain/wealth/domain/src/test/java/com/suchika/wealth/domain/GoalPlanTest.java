package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoalPlanTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID CHILD_ID = UUID.randomUUID();

    @Test
    void create_debtCrossover_minimalFields_succeeds() {
        GoalPlan plan = GoalPlan.create(ADMIN_ID, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null,
                "Reach a state where MF corpus exceeds outstanding debt", null, null, null, null, null));

        assertEquals(ADMIN_ID, plan.getAdminId());
        assertEquals(GoalType.DEBT_CROSSOVER, plan.getGoalType());
        assertNull(plan.getBeneficiaryProfileId());
        assertTrue(plan.isActive());
        assertNotNull(plan.getMilestones());
        assertTrue(plan.getMilestones().isEmpty());
        assertNotNull(plan.getDetail());
    }

    @Test
    void create_yearOne_requiresBeneficiaryProfileId() {
        GoalPlan.Spec spec = new GoalPlan.Spec(GoalType.YEAR_ONE, null, "Fund year one of college", null, null,
                new BigDecimal("1000000"), new BigDecimal("0.06"), 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(ADMIN_ID, spec));

        assertTrue(ex.getMessage().contains("beneficiary_profile_id"));
    }

    @Test
    void create_yearOne_withBeneficiaryProfileId_succeeds() {
        GoalPlan plan = GoalPlan.create(ADMIN_ID, new GoalPlan.Spec(GoalType.YEAR_ONE, CHILD_ID,
                "Fund year one of college", null, null, new BigDecimal("1000000"), new BigDecimal("0.06"), 10));

        assertEquals(CHILD_ID, plan.getBeneficiaryProfileId());
        assertEquals(0, new BigDecimal("1000000").compareTo(plan.getEducationBaseCost()));
        assertEquals(10, plan.getEducationYearsToEntry());
    }

    @Test
    void create_nonYearOne_rejectsBeneficiaryProfileId() {
        GoalPlan.Spec spec = new GoalPlan.Spec(GoalType.FREEDOM_RUNWAY, CHILD_ID, "Objective",
                null, null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(ADMIN_ID, spec));

        assertTrue(ex.getMessage().contains("must be null"));
    }

    @Test
    void create_nonYearOne_rejectsEducationFields() {
        GoalPlan.Spec spec = new GoalPlan.Spec(GoalType.INSURANCE_FREE, null, "Objective", null, null,
                new BigDecimal("1000000"), null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(ADMIN_ID, spec));

        assertTrue(ex.getMessage().contains("education_"));
    }

    @Test
    void create_nullAdminId_throws() {
        GoalPlan.Spec spec = new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null, "Objective",
                null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(null, spec));
    }

    @Test
    void create_nullGoalType_throws() {
        GoalPlan.Spec spec = new GoalPlan.Spec(null, null, "Objective", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(ADMIN_ID, spec));
    }

    @Test
    void create_blankObjective_throws() {
        GoalPlan.Spec spec = new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null, "   ",
                null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.create(ADMIN_ID, spec));
    }

    @Test
    void validateUniqueSequence_duplicateSequenceNumbers_throws() {
        List<Integer> sequenceNumbers = List.of(0, 1, 1, 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                GoalPlan.validateUniqueSequence(sequenceNumbers, "milestones"));

        assertTrue(ex.getMessage().contains("milestones"));
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    void validateUniqueSequence_uniqueSequenceNumbers_doesNotThrow() {
        assertDoesNotThrow(() -> GoalPlan.validateUniqueSequence(List.of(0, 1, 2, 3), "rules"));
    }

    @Test
    void validateUniqueSequence_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> GoalPlan.validateUniqueSequence(List.of(), "trigger_events"));
    }

    @Test
    void setters_mutateMutableFieldsOnly() {
        GoalPlan plan = GoalPlan.create(ADMIN_ID, new GoalPlan.Spec(GoalType.DEBT_CROSSOVER, null,
                "Objective", null, null, null, null, null));

        plan.setObjective("Updated objective");
        plan.setTargetState("Debt-free by 2030");
        plan.setAssumedGrowthRate(new BigDecimal("0.08"));
        plan.setActive(false);

        assertEquals("Updated objective", plan.getObjective());
        assertEquals("Debt-free by 2030", plan.getTargetState());
        assertEquals(0, new BigDecimal("0.08").compareTo(plan.getAssumedGrowthRate()));
        assertFalse(plan.isActive());
        // admin_id / goal_type remain immutable — no setters exist for them.
        assertEquals(ADMIN_ID, plan.getAdminId());
        assertEquals(GoalType.DEBT_CROSSOVER, plan.getGoalType());
    }

    @Test
    void builder_roundTripsAllFields() {
        UUID id = UUID.randomUUID();
        GoalPlan plan = GoalPlan.builder()
                .id(id)
                .adminId(ADMIN_ID)
                .goalType(GoalType.YEAR_ONE)
                .beneficiaryProfileId(CHILD_ID)
                .objective("Objective")
                .targetState("Target state")
                .assumedGrowthRate(new BigDecimal("0.1"))
                .educationBaseCost(new BigDecimal("500000"))
                .educationInflationRate(new BigDecimal("0.05"))
                .educationYearsToEntry(5)
                .active(true)
                .build();

        assertEquals(id, plan.getId());
        assertEquals(CHILD_ID, plan.getBeneficiaryProfileId());
        assertEquals("Target state", plan.getTargetState());
        assertEquals(5, plan.getEducationYearsToEntry());
    }
}
