package com.suchika.health.adapters.http.dto;

import java.time.LocalDate;
import java.util.UUID;

public class CreateDoctorVisitRequest {
    public UUID profileId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public Boolean visitedDoctor;
    public String doctorName;
    public String hospitalName;
    public String speciality;
    public String symptoms;
    public String diagnosis;
    public String notes;
    public LocalDate followUpDate;
}
