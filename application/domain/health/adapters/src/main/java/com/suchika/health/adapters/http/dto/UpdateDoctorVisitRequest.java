package com.suchika.health.adapters.http.dto;

import java.time.LocalDate;

public class UpdateDoctorVisitRequest {
    public LocalDate toDate;
    public String doctorName;
    public String hospitalName;
    public String speciality;
    public String symptoms;
    public String diagnosis;
    public String notes;
    public LocalDate followUpDate;
}
