package com.suchika.health.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class VitalReading {

    private UUID id;
    private UUID profileId;
    private VitalType vitalType;
    private LocalDate readingDate;
    private BigDecimal valuePrimary;
    private BigDecimal valueSecondary;
    private String unit;
    private String notes;
    private Instant createdAt;

    public VitalReading() {}

    private VitalReading(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.vitalType = builder.vitalType;
        this.readingDate = builder.readingDate;
        this.valuePrimary = builder.valuePrimary;
        this.valueSecondary = builder.valueSecondary;
        this.unit = builder.unit;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private VitalType vitalType;
        private LocalDate readingDate;
        private BigDecimal valuePrimary;
        private BigDecimal valueSecondary;
        private String unit;
        private String notes;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder vitalType(VitalType vitalType) { this.vitalType = vitalType; return this; }
        public Builder readingDate(LocalDate readingDate) { this.readingDate = readingDate; return this; }
        public Builder valuePrimary(BigDecimal valuePrimary) { this.valuePrimary = valuePrimary; return this; }
        public Builder valueSecondary(BigDecimal valueSecondary) { this.valueSecondary = valueSecondary; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public VitalReading build() { return new VitalReading(this); }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public VitalType getVitalType() { return vitalType; }
    public LocalDate getReadingDate() { return readingDate; }
    public BigDecimal getValuePrimary() { return valuePrimary; }
    public BigDecimal getValueSecondary() { return valueSecondary; }
    public String getUnit() { return unit; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
