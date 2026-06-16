package com.suchika.health.ports.input;

import java.time.LocalDate;
import java.util.UUID;

public record CreateDoctorVisitCommand(
        UUID profileId,
        LocalDate fromDate,
        LocalDate toDate,
        boolean visitedDoctor,
        String doctorName,
        String hospitalName,
        String speciality,
        String symptoms,
        String diagnosis,
        String notes,
        LocalDate followUpDate
) {}
