package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.UUID;

@RegisterForReflection
public class CreateGoalPlanRequest {

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
}
