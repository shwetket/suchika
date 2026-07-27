package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vital_reading", schema = "health")
public class VitalReadingEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", nullable = false, columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "vital_type", nullable = false, length = 30)
    public String vitalType;

    @Column(name = "reading_date", nullable = false)
    public LocalDate readingDate;

    @Column(name = "value_primary", nullable = false, precision = 8, scale = 2)
    public BigDecimal valuePrimary;

    @Column(name = "value_secondary", precision = 8, scale = 2)
    public BigDecimal valueSecondary;

    @Column(name = "unit", nullable = false, length = 20)
    public String unit;

    @Column(name = "notes")
    public String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static VitalReadingEntity from(VitalReading reading) {
        VitalReadingEntity e = new VitalReadingEntity();
        e.id = reading.getId();
        e.profileId = reading.getProfileId();
        e.vitalType = reading.getVitalType().name();
        e.readingDate = reading.getReadingDate();
        e.valuePrimary = reading.getValuePrimary();
        e.valueSecondary = reading.getValueSecondary();
        e.unit = reading.getUnit();
        e.notes = reading.getNotes();
        e.createdAt = reading.getCreatedAt();
        return e;
    }

    public VitalReading toDomain() {
        return VitalReading.builder()
                .id(id)
                .profileId(profileId)
                .vitalType(VitalType.valueOf(vitalType))
                .readingDate(readingDate)
                .valuePrimary(valuePrimary)
                .valueSecondary(valueSecondary)
                .unit(unit)
                .notes(notes)
                .createdAt(createdAt)
                .build();
    }
}
