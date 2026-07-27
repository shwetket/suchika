package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;

@RegisterForReflection
public class UpdateGoalRequest {

    @JsonProperty("goal_name")
    public String goalName;

    @JsonProperty("target_amount")
    public BigDecimal targetAmount;

    @JsonProperty("monthly_saving")
    public BigDecimal monthlySaving;

    @JsonProperty("target_date")
    public LocalDate targetDate;

    @JsonProperty("status")
    public String status;

    @JsonProperty("notes")
    public String notes;
}
