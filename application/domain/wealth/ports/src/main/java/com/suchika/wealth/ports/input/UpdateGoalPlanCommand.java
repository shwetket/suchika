package com.suchika.wealth.ports.input;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Partial-update payload for {@link GoalPlanUseCase#updateGoalPlan}. Non-null fields
 * replace the existing value; detail, if non-null, merges into the existing map
 * (mirrors the Account/PhysicalAsset metadata merge pattern). goal_type,
 * beneficiary_profile_id and admin_id are immutable after creation — not present here.
 */
public record UpdateGoalPlanCommand(
        String objective,
        String targetState,
        BigDecimal assumedGrowthRate,
        BigDecimal educationBaseCost,
        BigDecimal educationInflationRate,
        Integer educationYearsToEntry,
        Map<String, String> detail,
        Boolean isActive
) {}
