package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

abstract class VitalReadingFields {

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
}
