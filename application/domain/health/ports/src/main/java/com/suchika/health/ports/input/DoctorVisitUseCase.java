package com.suchika.health.ports.input;

import com.suchika.health.domain.DoctorVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorVisitUseCase {

    DoctorVisit create(CreateDoctorVisitCommand command);

    DoctorVisit getById(UUID id);

    /**
     * Lists visits for a profile, newest first. from/to (v0.6 UX item) optionally
     * filter by the visit's from_date falling within the given range; either may
     * be null to leave that side of the range open.
     */
    List<DoctorVisit> listByProfile(UUID profileId, LocalDate from, LocalDate to);

    DoctorVisit update(UUID id, UpdateDoctorVisitCommand command);

    void delete(UUID id);
}
