package com.suchika.health.adapters.http.dto;

import com.suchika.health.domain.VitalReading;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class VitalReadingResponse {

    public UUID id;
    public UUID profileId;
    public String vitalType;
    public LocalDate readingDate;
    public BigDecimal valuePrimary;
    public BigDecimal valueSecondary;
    public String unit;
    public String notes;
    public Instant createdAt;

    public static VitalReadingResponse from(VitalReading reading) {
        VitalReadingResponse r = new VitalReadingResponse();
        r.id = reading.getId();
        r.profileId = reading.getProfileId();
        r.vitalType = reading.getVitalType().name();
        r.readingDate = reading.getReadingDate();
        r.valuePrimary = reading.getValuePrimary();
        r.valueSecondary = reading.getValueSecondary();
        r.unit = reading.getUnit();
        r.notes = reading.getNotes();
        r.createdAt = reading.getCreatedAt();
        return r;
    }
}
