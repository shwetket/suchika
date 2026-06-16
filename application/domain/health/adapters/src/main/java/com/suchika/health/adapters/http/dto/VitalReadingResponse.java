package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.health.domain.VitalReading;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitalReadingResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("vital_type")
    public String vitalType;

    @JsonProperty("reading_date")
    public LocalDate readingDate;

    @JsonProperty("value_primary")
    public BigDecimal valuePrimary;

    @JsonProperty("value_secondary")
    public BigDecimal valueSecondary;

    @JsonProperty("unit")
    public String unit;

    @JsonProperty("notes")
    public String notes;

    @JsonProperty("created_at")
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
