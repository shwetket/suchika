package com.suchika.health.ports.input;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VitalReadingUseCase {

    VitalReading recordReading(UUID profileId, VitalType vitalType, LocalDate readingDate,
                        BigDecimal valuePrimary, BigDecimal valueSecondary,
                        String unit, String notes);

    VitalReading getById(UUID id);

    /** Returns all readings for the profile, newest first. Optional type filter. */
    List<VitalReading> listByProfile(UUID profileId, VitalType vitalType);

    /**
     * Paginated variant of {@link #listByProfile} — pre-v1.0 pagination pass (Q54).
     * {@code page} is 0-indexed. Used by the HTTP list endpoint; {@link #listByProfile}
     * stays as-is for any caller that wants the full list.
     */
    PagedVitalReadings listByProfilePaginated(UUID profileId, VitalType vitalType, int page, int size);

    VitalReading update(UUID id, UpdateVitalReadingCommand command);

    void delete(UUID id);
}
