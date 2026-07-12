package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.GoalPlan;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalPlanResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("admin_id")
    public UUID adminId;

    @JsonProperty("goal_type")
    public String goalType;

    @JsonProperty("beneficiary_profile_id")
    public UUID beneficiaryProfileId;

    @JsonProperty("objective")
    public String objective;

    @JsonProperty("target_state")
    public String targetState;

    @JsonProperty("assumed_growth_rate")
    public BigDecimal assumedGrowthRate;

    @JsonProperty("education_base_cost")
    public BigDecimal educationBaseCost;

    @JsonProperty("education_inflation_rate")
    public BigDecimal educationInflationRate;

    @JsonProperty("education_years_to_entry")
    public Integer educationYearsToEntry;

    @JsonProperty("detail")
    public Map<String, String> detail;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("milestones")
    public List<GoalMilestoneDto> milestones;

    @JsonProperty("rules")
    public List<GoalRuleDto> rules;

    @JsonProperty("trigger_events")
    public List<GoalTriggerEventDto> triggerEvents;

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("updated_at")
    public Instant updatedAt;

    public static GoalPlanResponse from(GoalPlan plan) {
        GoalPlanResponse r = new GoalPlanResponse();
        r.id = plan.getId();
        r.adminId = plan.getAdminId();
        r.goalType = plan.getGoalType() != null ? plan.getGoalType().name() : null;
        r.beneficiaryProfileId = plan.getBeneficiaryProfileId();
        r.objective = plan.getObjective();
        r.targetState = plan.getTargetState();
        r.assumedGrowthRate = plan.getAssumedGrowthRate();
        r.educationBaseCost = plan.getEducationBaseCost();
        r.educationInflationRate = plan.getEducationInflationRate();
        r.educationYearsToEntry = plan.getEducationYearsToEntry();
        r.detail = plan.getDetail();
        r.active = plan.isActive();
        r.milestones = plan.getMilestones().stream().map(GoalMilestoneDto::from).toList();
        r.rules = plan.getRules().stream().map(GoalRuleDto::from).toList();
        r.triggerEvents = plan.getTriggerEvents().stream().map(GoalTriggerEventDto::from).toList();
        r.createdAt = plan.getCreatedAt();
        r.updatedAt = plan.getUpdatedAt();
        return r;
    }
}
