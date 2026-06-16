package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.health.domain.VitalReading;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VitalReadingResponse extends VitalReadingFields {

    @JsonProperty("id")
    public UUID id;

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
