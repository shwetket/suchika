package com.suchika.health.ports.input;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VitalReadingUseCase {

    VitalReading record(UUID profileId, VitalType vitalType, LocalDate readingDate,
                        BigDecimal valuePrimary, BigDecimal valueSecondary,
                        String unit, String notes);

    VitalReading getById(UUID id);

    /** Returns all readings for the profile, newest first. Optional type filter. */
    List<VitalReading> listByProfile(UUID profileId, VitalType vitalType);

    void delete(UUID id);
}
