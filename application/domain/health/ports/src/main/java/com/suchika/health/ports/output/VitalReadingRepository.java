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

    /**
     * Paginated variant of {@link #findByProfileId} — pre-v1.0 pagination pass (Q54).
     * {@code page} is 0-indexed. Same filter predicate as the unpaginated method;
     * use {@link #countByProfileId} for the total matching count.
     */
    List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType, int page, int size);

    /**
     * Total count of readings matching the same filter predicate as
     * {@link #findByProfileId}, ignoring pagination — used to compute total pages.
     */
    long countByProfileId(UUID profileId, VitalType vitalType);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
