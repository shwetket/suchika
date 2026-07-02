package com.suchika.health.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;

@RegisterForReflection
public class UpdateVitalReadingRequest {

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
