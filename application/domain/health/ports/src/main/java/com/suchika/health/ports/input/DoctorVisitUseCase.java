package com.suchika.health.ports.input;

import com.suchika.health.domain.DoctorVisit;

import java.util.List;
import java.util.UUID;

public interface DoctorVisitUseCase {

    DoctorVisit create(CreateDoctorVisitCommand command);

    DoctorVisit getById(UUID id);

    List<DoctorVisit> listByProfile(UUID profileId);

    DoctorVisit update(UUID id, UpdateDoctorVisitCommand command);

    void delete(UUID id);
}
