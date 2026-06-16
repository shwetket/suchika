package com.suchika.health.adapters.http.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class RecordVitalReadingRequest {
    public UUID profileId;
    public String vitalType;
    public LocalDate readingDate;
    public BigDecimal valuePrimary;
    public BigDecimal valueSecondary;
    public String unit;
    public String notes;
}
