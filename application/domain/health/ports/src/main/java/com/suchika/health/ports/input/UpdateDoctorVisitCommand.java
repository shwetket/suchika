package com.suchika.health.ports.input;

import java.time.LocalDate;

public record UpdateDoctorVisitCommand(
        LocalDate toDate,
        String doctorName,
        String hospitalName,
        String speciality,
        String symptoms,
        String diagnosis,
        String notes,
        LocalDate followUpDate
) {}
