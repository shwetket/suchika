package com.suchika.health.ports.output;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VitalReadingRepository {

    VitalReading save(VitalReading reading);

    Optional<VitalReading> findById(UUID id);

    /** Newest first. vitalType may be null (no filter). */
    List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
