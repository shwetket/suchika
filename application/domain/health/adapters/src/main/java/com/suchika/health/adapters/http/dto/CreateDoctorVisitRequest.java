package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
public class CreateDoctorVisitRequest extends DoctorVisitFields {

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("from_date")
    public LocalDate fromDate;

    @JsonProperty("visited_doctor")
    public Boolean visitedDoctor;
}
