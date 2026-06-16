package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
public class CreateDoctorVisitRequest {

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("from_date")
    public LocalDate fromDate;

    @JsonProperty("to_date")
    public LocalDate toDate;

    @JsonProperty("visited_doctor")
    public Boolean visitedDoctor;

    @JsonProperty("doctor_name")
    public String doctorName;

    @JsonProperty("hospital_name")
    public String hospitalName;

    @JsonProperty("speciality")
    public String speciality;

    @JsonProperty("symptoms")
    public String symptoms;

    @JsonProperty("diagnosis")
    public String diagnosis;

    @JsonProperty("notes")
    public String notes;

    @JsonProperty("follow_up_date")
    public LocalDate followUpDate;
}
