package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.CreateGoalPlanRequest;
import com.suchika.wealth.adapters.http.dto.GoalMilestoneDto;
import com.suchika.wealth.adapters.http.dto.GoalPlanResponse;
import com.suchika.wealth.adapters.http.dto.GoalRuleDto;
import com.suchika.wealth.adapters.http.dto.GoalTriggerEventDto;
import com.suchika.wealth.adapters.http.dto.ListGoalPlansResponse;
import com.suchika.wealth.adapters.http.dto.UpdateGoalPlanRequest;
import com.suchika.wealth.adapters.http.dto.UpdateMilestoneAchievedRequest;
import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalPlan;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;
import com.suchika.wealth.ports.input.CreateGoalPlanCommand;
import com.suchika.wealth.ports.input.GoalPlanUseCase;
import com.suchika.wealth.ports.input.UpdateGoalPlanCommand;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalPlanResourceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID GOAL_PLAN_ID = UUID.randomUUID();
    private static final UUID MILESTONE_ID = UUID.randomUUID();

    private GoalPlanResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new GoalPlanResource(useCase);
    }

    @Test
    void listGoalPlans_returns200_withGoalPlanList() {
        useCase.plansToReturn = List.of(buildPlan());

        Response response = resource.listGoalPlans(ADMIN_ID);

        assertEquals(200, response.getStatus());
        ListGoalPlansResponse body = (ListGoalPlansResponse) response.getEntity();
        assertEquals(1, body.goalPlans.size());
        assertEquals(ADMIN_ID, useCase.lastListAdminId);
    }

    @Test
    void listGoalPlans_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listGoalPlans(null));
    }

    @Test
    void createGoalPlan_returns201_withCreatedPlan() {
        CreateGoalPlanRequest request = new CreateGoalPlanRequest();
        request.goalType = "DEBT_CROSSOVER";
        request.objective = "Reach a debt-free state";
        useCase.planToReturn = buildPlan();

        Response response = resource.createGoalPlan(ADMIN_ID, request);

        assertEquals(201, response.getStatus());
        assertEquals(GoalType.DEBT_CROSSOVER, useCase.lastCreateCommand.goalType());
        assertEquals("Reach a debt-free state", useCase.lastCreateCommand.objective());
    }

    @Test
    void createGoalPlan_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.createGoalPlan(ADMIN_ID, null));
    }

    @Test
    void createGoalPlan_missingAdminId_throwsBadRequest() {
        CreateGoalPlanRequest request = new CreateGoalPlanRequest();
        request.goalType = "DEBT_CROSSOVER";
        request.objective = "Objective";
        assertThrows(BadRequestException.class, () -> resource.createGoalPlan(null, request));
    }

    @Test
    void createGoalPlan_invalidGoalType_throwsBadRequest() {
        CreateGoalPlanRequest request = new CreateGoalPlanRequest();
        request.goalType = "NOT_A_TYPE";
        request.objective = "Objective";
        assertThrows(BadRequestException.class, () -> resource.createGoalPlan(ADMIN_ID, request));
    }

    @Test
    void getGoalPlan_returnsGoalPlanResponse() {
        useCase.planToReturn = buildPlan();

        GoalPlanResponse response = resource.getGoalPlan(GOAL_PLAN_ID, ADMIN_ID);

        assertEquals("DEBT_CROSSOVER", response.goalType);
        assertEquals(ADMIN_ID, useCase.lastGetAdminId);
    }

    @Test
    void getGoalPlan_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.getGoalPlan(GOAL_PLAN_ID, null));
    }

    @Test
    void updateGoalPlan_returnsUpdatedPlan() {
        UpdateGoalPlanRequest request = new UpdateGoalPlanRequest();
        request.objective = "Updated objective";
        useCase.planToReturn = buildPlan();

        GoalPlanResponse response = resource.updateGoalPlan(GOAL_PLAN_ID, ADMIN_ID, request);

        assertEquals("DEBT_CROSSOVER", response.goalType);
        assertEquals(GOAL_PLAN_ID, useCase.lastUpdateGoalPlanId);
        assertEquals("Updated objective", useCase.lastUpdateCommand.objective());
    }

    @Test
    void updateGoalPlan_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateGoalPlan(GOAL_PLAN_ID, ADMIN_ID, null));
    }

    @Test
    void deactivateGoalPlan_returns204() {
        Response response = resource.deactivateGoalPlan(GOAL_PLAN_ID, ADMIN_ID);

        assertEquals(204, response.getStatus());
        assertEquals(GOAL_PLAN_ID, useCase.lastDeactivateGoalPlanId);
        assertEquals(ADMIN_ID, useCase.lastDeactivateAdminId);
    }

    @Test
    void deactivateGoalPlan_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.deactivateGoalPlan(GOAL_PLAN_ID, null));
    }

    @Test
    void replaceMilestones_returnsReplacedList() {
        GoalMilestoneDto dto = new GoalMilestoneDto();
        dto.sequenceNo = 0;
        dto.label = "First milestone";
        dto.targetValue = new BigDecimal("50");
        dto.significance = "Halfway point";
        useCase.milestonesToReturn = List.of(dto.toDomain());

        List<GoalMilestoneDto> response = resource.replaceMilestones(GOAL_PLAN_ID, ADMIN_ID, List.of(dto));

        assertEquals(1, response.size());
        assertEquals("First milestone", response.get(0).label);
        assertEquals(1, useCase.lastMilestones.size());
    }

    @Test
    void replaceMilestones_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.replaceMilestones(GOAL_PLAN_ID, ADMIN_ID, null));
    }

    @Test
    void updateMilestoneAchieved_returnsUpdatedMilestone() {
        UpdateMilestoneAchievedRequest request = new UpdateMilestoneAchievedRequest();
        request.achieved = true;
        useCase.milestoneToReturn = GoalMilestone.create(MILESTONE_ID, 0, "Checklist item", null, true, true, "Manual step");

        GoalMilestoneDto response = resource.updateMilestoneAchieved(GOAL_PLAN_ID, MILESTONE_ID, ADMIN_ID, request);

        assertTrue(response.achieved);
        assertEquals(MILESTONE_ID, useCase.lastMilestoneId);
        assertEquals(Boolean.TRUE, useCase.lastMilestoneAchieved);
    }

    @Test
    void updateMilestoneAchieved_missingIsAchieved_throwsBadRequest() {
        UpdateMilestoneAchievedRequest request = new UpdateMilestoneAchievedRequest();
        assertThrows(BadRequestException.class,
                () -> resource.updateMilestoneAchieved(GOAL_PLAN_ID, MILESTONE_ID, ADMIN_ID, request));
    }

    @Test
    void replaceRules_returnsReplacedList() {
        GoalRuleDto dto = new GoalRuleDto();
        dto.sequenceNo = 0;
        dto.ruleName = "No Liquidation";
        dto.ruleText = "Never liquidate mid-goal";
        useCase.rulesToReturn = List.of(dto.toDomain());

        List<GoalRuleDto> response = resource.replaceRules(GOAL_PLAN_ID, ADMIN_ID, List.of(dto));

        assertEquals(1, response.size());
        assertEquals("No Liquidation", response.get(0).ruleName);
    }

    @Test
    void replaceTriggerEvents_returnsReplacedList() {
        GoalTriggerEventDto dto = new GoalTriggerEventDto();
        dto.sequenceNo = 0;
        dto.eventName = "Bonus received";
        dto.triggerCondition = "Annual bonus exceeds 1 lakh";
        dto.resultingChange = "Allocate 50% to MF corpus";
        useCase.triggerEventsToReturn = List.of(dto.toDomain());

        List<GoalTriggerEventDto> response = resource.replaceTriggerEvents(GOAL_PLAN_ID, ADMIN_ID, List.of(dto));

        assertEquals(1, response.size());
        assertEquals("Bonus received", response.get(0).eventName);
    }

    private GoalPlan buildPlan() {
        return GoalPlan.builder()
                .id(GOAL_PLAN_ID)
                .adminId(ADMIN_ID)
                .goalType(GoalType.DEBT_CROSSOVER)
                .objective("Objective")
                .active(true)
                .build();
    }

    static class StubUseCase implements GoalPlanUseCase {
        List<GoalPlan> plansToReturn = List.of();
        GoalPlan planToReturn;
        List<GoalMilestone> milestonesToReturn = List.of();
        List<GoalRule> rulesToReturn = List.of();
        List<GoalTriggerEvent> triggerEventsToReturn = List.of();
        GoalMilestone milestoneToReturn;

        UUID lastListAdminId;
        CreateGoalPlanCommand lastCreateCommand;
        UUID lastGetAdminId;
        UUID lastUpdateGoalPlanId;
        UpdateGoalPlanCommand lastUpdateCommand;
        UUID lastDeactivateGoalPlanId;
        UUID lastDeactivateAdminId;
        List<GoalMilestone> lastMilestones;
        UUID lastMilestoneId;
        Boolean lastMilestoneAchieved;

        @Override
        public GoalPlan createGoalPlan(UUID adminId, CreateGoalPlanCommand command) {
            lastCreateCommand = command;
            return planToReturn;
        }

        @Override
        public GoalPlan getGoalPlan(UUID id, UUID adminId) {
            lastGetAdminId = adminId;
            return planToReturn;
        }

        @Override
        public List<GoalPlan> listGoalPlans(UUID adminId) {
            lastListAdminId = adminId;
            return plansToReturn;
        }

        @Override
        public GoalPlan updateGoalPlan(UUID id, UUID adminId, UpdateGoalPlanCommand command) {
            lastUpdateGoalPlanId = id;
            lastUpdateCommand = command;
            return planToReturn;
        }

        @Override
        public void deactivateGoalPlan(UUID id, UUID adminId) {
            lastDeactivateGoalPlanId = id;
            lastDeactivateAdminId = adminId;
        }

        @Override
        public List<GoalMilestone> replaceMilestones(UUID goalPlanId, UUID adminId, List<GoalMilestone> milestones) {
            lastMilestones = milestones;
            return milestonesToReturn;
        }

        @Override
        public List<GoalRule> replaceRules(UUID goalPlanId, UUID adminId, List<GoalRule> rules) {
            return rulesToReturn;
        }

        @Override
        public List<GoalTriggerEvent> replaceTriggerEvents(UUID goalPlanId, UUID adminId, List<GoalTriggerEvent> triggerEvents) {
            return triggerEventsToReturn;
        }

        @Override
        public GoalMilestone updateMilestoneAchieved(UUID goalPlanId, UUID adminId, UUID milestoneId, boolean achieved) {
            lastMilestoneId = milestoneId;
            lastMilestoneAchieved = achieved;
            return milestoneToReturn;
        }
    }
}
