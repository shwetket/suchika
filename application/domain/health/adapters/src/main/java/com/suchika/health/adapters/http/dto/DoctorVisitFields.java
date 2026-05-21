package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

abstract class DoctorVisitFields {

    @JsonProperty("to_date")
    public LocalDate toDate;

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
