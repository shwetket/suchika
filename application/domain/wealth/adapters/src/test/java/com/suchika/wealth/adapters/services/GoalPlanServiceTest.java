package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;
import com.suchika.wealth.ports.input.CreateGoalPlanCommand;
import com.suchika.wealth.ports.input.UpdateGoalPlanCommand;
import com.suchika.wealth.ports.output.GoalPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GoalPlanServiceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private GoalPlanService service;
    private FakeGoalPlanRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FakeGoalPlanRepository();
        service = new GoalPlanService(repo);
    }

    private static CreateGoalPlanCommand cmd(GoalType goalType, UUID beneficiaryProfileId, String objective) {
        return new CreateGoalPlanCommand(goalType, beneficiaryProfileId, objective, null, null, null, null, null);
    }

    @Test
    void createGoalPlan_happyPath_returnsPlanWithId() {
        GoalPlan result = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));

        assertNotNull(result.getId());
        assertEquals(ADMIN_ID, result.getAdminId());
        assertEquals(GoalType.DEBT_CROSSOVER, result.getGoalType());
        assertTrue(result.isActive());
    }

    @Test
    void createGoalPlan_duplicateSingletonType_throwsConflict() {
        service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "First"));

        assertThrows(ConflictException.class, () ->
                service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Second")));
    }

    @Test
    void createGoalPlan_yearOne_differentChildren_bothSucceed() {
        UUID child1 = UUID.randomUUID();
        UUID child2 = UUID.randomUUID();

        GoalPlan plan1 = service.createGoalPlan(ADMIN_ID, cmd(GoalType.YEAR_ONE, child1, "Fund child 1"));
        GoalPlan plan2 = service.createGoalPlan(ADMIN_ID, cmd(GoalType.YEAR_ONE, child2, "Fund child 2"));

        assertNotEquals(plan1.getId(), plan2.getId());
    }

    @Test
    void createGoalPlan_invalidGoalPlan_domainValidationThrowsIllegalArgument() {
        // YEAR_ONE without a beneficiary_profile_id — domain-layer validation (ADR-020)
        assertThrows(IllegalArgumentException.class, () ->
                service.createGoalPlan(ADMIN_ID, cmd(GoalType.YEAR_ONE, null, "Objective")));
    }

    @Test
    void getGoalPlan_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getGoalPlan(UUID.randomUUID(), ADMIN_ID));
    }

    @Test
    void getGoalPlan_wrongAdmin_throwsNotFound() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));

        assertThrows(NotFoundException.class, () -> service.getGoalPlan(plan.getId(), UUID.randomUUID()));
    }

    @Test
    void getGoalPlan_attachesChildCollections() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        service.replaceMilestones(plan.getId(), ADMIN_ID, List.of(
                GoalMilestone.create(null, 0, "Milestone", new BigDecimal("50"), false, false, "Significance")));

        GoalPlan found = service.getGoalPlan(plan.getId(), ADMIN_ID);

        assertEquals(1, found.getMilestones().size());
    }

    @Test
    void updateGoalPlan_mergesDetailMap_doesNotReplaceWholesale() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        Map<String, String> firstDetail = new HashMap<>();
        firstDetail.put("baseline_mf_corpus", "500000");
        service.updateGoalPlan(plan.getId(), ADMIN_ID, new UpdateGoalPlanCommand(
                null, null, null, null, null, null, firstDetail, null));

        Map<String, String> secondDetail = new HashMap<>();
        secondDetail.put("baseline_debt", "300000");
        GoalPlan updated = service.updateGoalPlan(plan.getId(), ADMIN_ID, new UpdateGoalPlanCommand(
                null, null, null, null, null, null, secondDetail, null));

        assertEquals("500000", updated.getDetail().get("baseline_mf_corpus"));
        assertEquals("300000", updated.getDetail().get("baseline_debt"));
    }

    @Test
    void updateGoalPlan_blankObjective_throwsBadRequest() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));

        assertThrows(BadRequestException.class, () -> service.updateGoalPlan(plan.getId(), ADMIN_ID,
                new UpdateGoalPlanCommand("  ", null, null, null, null, null, null, null)));
    }

    @Test
    void deactivateGoalPlan_setsActiveFalse() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));

        service.deactivateGoalPlan(plan.getId(), ADMIN_ID);

        assertFalse(service.getGoalPlan(plan.getId(), ADMIN_ID).isActive());
    }

    @Test
    void replaceMilestones_duplicateSequenceNo_throwsIllegalArgument() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        List<GoalMilestone> milestones = List.of(
                GoalMilestone.create(null, 0, "First", new BigDecimal("25"), false, false, "Sig1"),
                GoalMilestone.create(null, 0, "Second", new BigDecimal("50"), false, false, "Sig2"));

        assertThrows(IllegalArgumentException.class, () -> service.replaceMilestones(plan.getId(), ADMIN_ID, milestones));
    }

    @Test
    void replaceMilestones_planNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () ->
                service.replaceMilestones(UUID.randomUUID(), ADMIN_ID, List.of()));
    }

    @Test
    void replaceRules_duplicateSequenceNo_throwsIllegalArgument() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        List<GoalRule> rules = List.of(
                GoalRule.create(null, 0, "Rule1", "Text1"),
                GoalRule.create(null, 0, "Rule2", "Text2"));

        assertThrows(IllegalArgumentException.class, () -> service.replaceRules(plan.getId(), ADMIN_ID, rules));
    }

    @Test
    void replaceTriggerEvents_duplicateSequenceNo_throwsIllegalArgument() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        List<GoalTriggerEvent> events = List.of(
                GoalTriggerEvent.create(null, 0, "Event1", "Condition1", "Change1"),
                GoalTriggerEvent.create(null, 0, "Event2", "Condition2", "Change2"));

        assertThrows(IllegalArgumentException.class, () -> service.replaceTriggerEvents(plan.getId(), ADMIN_ID, events));
    }

    @Test
    void updateMilestoneAchieved_manualChecklist_togglesSuccessfully() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        List<GoalMilestone> saved = service.replaceMilestones(plan.getId(), ADMIN_ID, List.of(
                GoalMilestone.create(null, 0, "Checklist item", null, true, false, "Manual step")));
        UUID milestoneId = saved.get(0).getId();

        GoalMilestone updated = service.updateMilestoneAchieved(plan.getId(), ADMIN_ID, milestoneId, true);

        assertTrue(updated.isAchieved());
    }

    @Test
    void updateMilestoneAchieved_nonChecklistMilestone_throwsBadRequest() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));
        List<GoalMilestone> saved = service.replaceMilestones(plan.getId(), ADMIN_ID, List.of(
                GoalMilestone.create(null, 0, "Formula milestone", new BigDecimal("50"), false, false, "Derived")));
        UUID milestoneId = saved.get(0).getId();

        assertThrows(BadRequestException.class, () ->
                service.updateMilestoneAchieved(plan.getId(), ADMIN_ID, milestoneId, true));
    }

    @Test
    void updateMilestoneAchieved_milestoneNotFound_throwsNotFound() {
        GoalPlan plan = service.createGoalPlan(ADMIN_ID, cmd(GoalType.DEBT_CROSSOVER, null, "Objective"));

        assertThrows(NotFoundException.class, () ->
                service.updateMilestoneAchieved(plan.getId(), ADMIN_ID, UUID.randomUUID(), true));
    }

    /**
     * Minimal in-memory fake — mirrors PhysicalAssetServiceTest's
     * FakePhysicalAssetRepository pattern (hand-written fake, not Mockito).
     */
    static class FakeGoalPlanRepository implements GoalPlanRepository {
        private final Map<UUID, GoalPlan> plans = new HashMap<>();
        private final Map<UUID, List<GoalMilestone>> milestones = new HashMap<>();
        private final Map<UUID, List<GoalRule>> rules = new HashMap<>();
        private final Map<UUID, List<GoalTriggerEvent>> triggerEvents = new HashMap<>();

        @Override
        public GoalPlan save(GoalPlan goalPlan) {
            UUID id = goalPlan.getId() != null ? goalPlan.getId() : UUID.randomUUID();
            GoalPlan stored = GoalPlan.builder()
                    .id(id)
                    .adminId(goalPlan.getAdminId())
                    .goalType(goalPlan.getGoalType())
                    .beneficiaryProfileId(goalPlan.getBeneficiaryProfileId())
                    .objective(goalPlan.getObjective())
                    .targetState(goalPlan.getTargetState())
                    .assumedGrowthRate(goalPlan.getAssumedGrowthRate())
                    .educationBaseCost(goalPlan.getEducationBaseCost())
                    .educationInflationRate(goalPlan.getEducationInflationRate())
                    .educationYearsToEntry(goalPlan.getEducationYearsToEntry())
                    .detail(new HashMap<>(goalPlan.getDetail()))
                    .active(goalPlan.isActive())
                    .createdAt(java.time.Instant.now())
                    .updatedAt(java.time.Instant.now())
                    .build();
            plans.put(id, stored);
            return stored;
        }

        @Override
        public Optional<GoalPlan> findById(UUID id, UUID adminId) {
            GoalPlan plan = plans.get(id);
            if (plan == null || !plan.getAdminId().equals(adminId)) {
                return Optional.empty();
            }
            return Optional.of(plan);
        }

        @Override
        public List<GoalPlan> findAll(UUID adminId) {
            return plans.values().stream().filter(p -> p.getAdminId().equals(adminId)).toList();
        }

        @Override
        public boolean existsByAdminGoalTypeBeneficiary(UUID adminId, GoalType goalType, UUID beneficiaryProfileId) {
            return plans.values().stream().anyMatch(p ->
                    p.getAdminId().equals(adminId) && p.getGoalType() == goalType
                            && java.util.Objects.equals(p.getBeneficiaryProfileId(), beneficiaryProfileId));
        }

        @Override
        public List<GoalMilestone> findMilestones(UUID goalPlanId) {
            return milestones.getOrDefault(goalPlanId, List.of());
        }

        @Override
        public List<GoalMilestone> replaceMilestones(UUID goalPlanId, List<GoalMilestone> newMilestones) {
            List<GoalMilestone> withIds = new ArrayList<>();
            for (GoalMilestone m : newMilestones) {
                withIds.add(GoalMilestone.create(
                        m.getId() != null ? m.getId() : UUID.randomUUID(),
                        m.getSequenceNo(), m.getLabel(), m.getTargetValue(),
                        m.isManualChecklist(), m.isAchieved(), m.getSignificance()));
            }
            milestones.put(goalPlanId, withIds);
            return withIds;
        }

        @Override
        public List<GoalRule> findRules(UUID goalPlanId) {
            return rules.getOrDefault(goalPlanId, List.of());
        }

        @Override
        public List<GoalRule> replaceRules(UUID goalPlanId, List<GoalRule> newRules) {
            rules.put(goalPlanId, new ArrayList<>(newRules));
            return newRules;
        }

        @Override
        public List<GoalTriggerEvent> findTriggerEvents(UUID goalPlanId) {
            return triggerEvents.getOrDefault(goalPlanId, List.of());
        }

        @Override
        public List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, List<GoalTriggerEvent> newTriggerEvents) {
            triggerEvents.put(goalPlanId, new ArrayList<>(newTriggerEvents));
            return newTriggerEvents;
        }

        @Override
        public Optional<GoalMilestone> findMilestoneById(UUID goalPlanId, UUID milestoneId) {
            return findMilestones(goalPlanId).stream().filter(m -> m.getId().equals(milestoneId)).findFirst();
        }

        @Override
        public GoalMilestone saveMilestone(UUID goalPlanId, GoalMilestone milestone) {
            List<GoalMilestone> current = new ArrayList<>(findMilestones(goalPlanId));
            current.removeIf(m -> m.getId().equals(milestone.getId()));
            current.add(milestone);
            milestones.put(goalPlanId, current);
            return milestone;
        }
    }
}
