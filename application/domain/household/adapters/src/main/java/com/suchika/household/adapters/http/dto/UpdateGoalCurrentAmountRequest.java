package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public class UpdateGoalCurrentAmountRequest {

    @JsonProperty("current_amount")
    public BigDecimal currentAmount;
}
