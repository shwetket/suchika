package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.utils.ResourceUtils;
import com.suchika.wealth.adapters.http.dto.CreateGoalPlanRequest;
import com.suchika.wealth.adapters.http.dto.GoalMilestoneDto;
import com.suchika.wealth.adapters.http.dto.GoalPlanResponse;
import com.suchika.wealth.adapters.http.dto.GoalRuleDto;
import com.suchika.wealth.adapters.http.dto.GoalTriggerEventDto;
import com.suchika.wealth.adapters.http.dto.ListGoalPlansResponse;
import com.suchika.wealth.adapters.http.dto.UpdateGoalPlanRequest;
import com.suchika.wealth.adapters.http.dto.UpdateMilestoneAchievedRequest;
import com.suchika.wealth.domain.GoalMilestone;
import com.suchika.wealth.domain.GoalRule;
import com.suchika.wealth.domain.GoalTriggerEvent;
import com.suchika.wealth.domain.GoalType;
import com.suchika.wealth.ports.input.CreateGoalPlanCommand;
import com.suchika.wealth.ports.input.GoalPlanUseCase;
import com.suchika.wealth.ports.input.UpdateGoalPlanCommand;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/goal-plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GoalPlanResource {

    private final GoalPlanUseCase useCase;

    public GoalPlanResource(GoalPlanUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listGoalPlans(@QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        List<GoalPlanResponse> plans = useCase.listGoalPlans(adminId).stream()
                .map(GoalPlanResponse::from).toList();
        return Response.ok(new ListGoalPlansResponse(plans)).build();
    }

    @POST
    public Response createGoalPlan(@QueryParam("admin_id") UUID adminId, CreateGoalPlanRequest request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        GoalType goalType = parseGoalType(request.goalType);
        CreateGoalPlanCommand command = new CreateGoalPlanCommand(
                goalType, request.beneficiaryProfileId, request.objective, request.targetState,
                request.assumedGrowthRate, request.educationBaseCost, request.educationInflationRate,
                request.educationYearsToEntry);
        return Response.status(201)
                .entity(GoalPlanResponse.from(useCase.createGoalPlan(adminId, command)))
                .build();
    }

    @GET
    @Path("/{goal_plan_id}")
    public GoalPlanResponse getGoalPlan(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        return GoalPlanResponse.from(useCase.getGoalPlan(goalPlanId, adminId));
    }

    @PATCH
    @Path("/{goal_plan_id}")
    public GoalPlanResponse updateGoalPlan(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            UpdateGoalPlanRequest request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        UpdateGoalPlanCommand command = new UpdateGoalPlanCommand(
                request.objective, request.targetState, request.assumedGrowthRate,
                request.educationBaseCost, request.educationInflationRate, request.educationYearsToEntry,
                request.detail, request.active);
        return GoalPlanResponse.from(useCase.updateGoalPlan(goalPlanId, adminId, command));
    }

    @DELETE
    @Path("/{goal_plan_id}")
    public Response deactivateGoalPlan(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        useCase.deactivateGoalPlan(goalPlanId, adminId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{goal_plan_id}/milestones")
    public List<GoalMilestoneDto> replaceMilestones(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            List<GoalMilestoneDto> request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        List<GoalMilestone> milestones = request.stream().map(GoalMilestoneDto::toDomain).toList();
        return useCase.replaceMilestones(goalPlanId, adminId, milestones).stream()
                .map(GoalMilestoneDto::from).toList();
    }

    @PATCH
    @Path("/{goal_plan_id}/milestones/{milestone_id}")
    public GoalMilestoneDto updateMilestoneAchieved(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @PathParam("milestone_id") UUID milestoneId,
            @QueryParam("admin_id") UUID adminId,
            UpdateMilestoneAchievedRequest request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null || request.achieved == null) {
            throw new BadRequestException("is_achieved is required");
        }
        return GoalMilestoneDto.from(
                useCase.updateMilestoneAchieved(goalPlanId, adminId, milestoneId, request.achieved));
    }

    @PUT
    @Path("/{goal_plan_id}/rules")
    public List<GoalRuleDto> replaceRules(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            List<GoalRuleDto> request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        List<GoalRule> rules = request.stream().map(GoalRuleDto::toDomain).toList();
        return useCase.replaceRules(goalPlanId, adminId, rules).stream().map(GoalRuleDto::from).toList();
    }

    @PUT
    @Path("/{goal_plan_id}/trigger-events")
    public List<GoalTriggerEventDto> replaceTriggerEvents(
            @PathParam("goal_plan_id") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            List<GoalTriggerEventDto> request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        List<GoalTriggerEvent> events = request.stream().map(GoalTriggerEventDto::toDomain).toList();
        return useCase.replaceTriggerEvents(goalPlanId, adminId, events).stream()
                .map(GoalTriggerEventDto::from).toList();
    }

    private GoalType parseGoalType(String value) {
        if (value == null) throw new BadRequestException("goal_type is required");
        try {
            return GoalType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid goal_type: " + value);
        }
    }
}
