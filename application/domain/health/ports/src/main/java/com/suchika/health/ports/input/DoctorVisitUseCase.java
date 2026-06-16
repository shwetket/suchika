package com.suchika.health.ports.input;

import com.suchika.health.domain.DoctorVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorVisitUseCase {

    DoctorVisit create(UUID profileId, LocalDate fromDate, LocalDate toDate,
                       boolean visitedDoctor, String doctorName, String hospitalName,
                       String speciality, String symptoms, String diagnosis,
                       String notes, LocalDate followUpDate);

    DoctorVisit getById(UUID id);

    List<DoctorVisit> listByProfile(UUID profileId);

    DoctorVisit update(UUID id, LocalDate toDate, String doctorName, String hospitalName,
                       String speciality, String symptoms, String diagnosis,
                       String notes, LocalDate followUpDate);

    void delete(UUID id);
}
