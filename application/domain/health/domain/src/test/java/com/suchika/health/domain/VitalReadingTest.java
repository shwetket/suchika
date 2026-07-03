package com.suchika.health.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VitalReadingTest {

    @Test
    void builder_setsAllFields() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        LocalDate readingDate = LocalDate.of(2026, Month.JUNE, 1);
        Instant createdAt = Instant.parse("2026-06-01T10:00:00Z");

        VitalReading reading = VitalReading.builder()
                .id(id)
                .profileId(profileId)
                .vitalType(VitalType.BLOOD_PRESSURE)
                .readingDate(readingDate)
                .valuePrimary(BigDecimal.valueOf(120))
                .valueSecondary(BigDecimal.valueOf(80))
                .unit("mmHg")
                .notes("morning reading")
                .createdAt(createdAt)
                .build();

        assertEquals(id, reading.getId());
        assertEquals(profileId, reading.getProfileId());
        assertEquals(VitalType.BLOOD_PRESSURE, reading.getVitalType());
        assertEquals(readingDate, reading.getReadingDate());
        assertEquals(BigDecimal.valueOf(120), reading.getValuePrimary());
        assertEquals(BigDecimal.valueOf(80), reading.getValueSecondary());
        assertEquals("mmHg", reading.getUnit());
        assertEquals("morning reading", reading.getNotes());
        assertEquals(createdAt, reading.getCreatedAt());
    }

    @Test
    void builder_singleValueVital_leavesValueSecondaryNull() {
        VitalReading reading = VitalReading.builder()
                .vitalType(VitalType.WEIGHT)
                .valuePrimary(BigDecimal.valueOf(70))
                .unit("kg")
                .build();

        assertEquals(BigDecimal.valueOf(70), reading.getValuePrimary());
        assertNull(reading.getValueSecondary());
    }

    @Test
    void noArgConstructor_leavesAllFieldsNull() {
        VitalReading reading = new VitalReading();

        assertNull(reading.getId());
        assertNull(reading.getVitalType());
        assertNull(reading.getValuePrimary());
    }
}
