package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.GoalType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * admin_id is passed separately to {@link GoalPlanUseCase#createGoalPlan}, mirroring
 * the CreateAccountCommand precedent (profileId kept out of the command, Sonar S107).
 */
public record CreateGoalPlanCommand(
        GoalType goalType,
        UUID beneficiaryProfileId,
        String objective,
        String targetState,
        BigDecimal assumedGrowthRate,
        BigDecimal educationBaseCost,
        BigDecimal educationInflationRate,
        Integer educationYearsToEntry
) {}
