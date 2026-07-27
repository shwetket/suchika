package com.suchika.gateway.vacationplanner;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;

@RegisterForReflection
public class VacationPlannerRequest {

    @JsonProperty("trip_cost")
    public double tripCost;

    @JsonProperty("trip_start_date")
    public LocalDate tripStartDate;

    @JsonProperty("trip_end_date")
    public LocalDate tripEndDate;
}
