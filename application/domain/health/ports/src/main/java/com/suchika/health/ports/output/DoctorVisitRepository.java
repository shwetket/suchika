package com.suchika.health.ports.output;

import com.suchika.health.domain.DoctorVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorVisitRepository {

    DoctorVisit save(DoctorVisit visit);

    Optional<DoctorVisit> findById(UUID id);

    /**
     * Lists visits for a profile, newest first, optionally filtered by from_date
     * falling within [from, to] (v0.6 UX item — date-range filter on doctor visit
     * list). Either bound may be null to leave that side of the range open.
     */
    List<DoctorVisit> findByProfileId(UUID profileId, LocalDate from, LocalDate to);

    /**
     * Paginated variant of {@link #findByProfileId} — pre-v1.0 pagination pass (Q54).
     * {@code page} is 0-indexed. Same filter predicate as the unpaginated method;
     * use {@link #countByProfileId} for the total matching count.
     */
    List<DoctorVisit> findByProfileId(UUID profileId, LocalDate from, LocalDate to, int page, int size);

    /**
     * Total count of visits matching the same filter predicate as
     * {@link #findByProfileId}, ignoring pagination — used to compute total pages.
     */
    long countByProfileId(UUID profileId, LocalDate from, LocalDate to);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
