package com.suchika.health.ports.output;

import com.suchika.health.domain.DoctorVisit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorVisitRepository {

    DoctorVisit save(DoctorVisit visit);

    Optional<DoctorVisit> findById(UUID id);

    List<DoctorVisit> findByProfileId(UUID profileId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
