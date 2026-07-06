package com.suchika.health.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VitalReadingTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final LocalDate READING_DATE = LocalDate.of(2026, Month.JULY, 1);

    @Test
    void create_happyPath_returnsReadingWithAllFields() {
        VitalReading reading = VitalReading.create(PROFILE_ID, VitalType.WEIGHT, READING_DATE,
                new BigDecimal("70.5"), null, "kg", "morning");

        assertNotNull(reading);
        assertEquals(PROFILE_ID, reading.getProfileId());
        assertEquals(VitalType.WEIGHT, reading.getVitalType());
        assertEquals(READING_DATE, reading.getReadingDate());
        assertEquals(new BigDecimal("70.5"), reading.getValuePrimary());
        assertNull(reading.getValueSecondary());
        assertEquals("kg", reading.getUnit());
        assertEquals("morning", reading.getNotes());
    }

    @Test
    void create_bloodPressureWithSecondaryValue_succeeds() {
        VitalReading reading = VitalReading.create(PROFILE_ID, VitalType.BLOOD_PRESSURE, READING_DATE,
                new BigDecimal("120"), new BigDecimal("80"), "mmHg", null);

        assertEquals(new BigDecimal("120"), reading.getValuePrimary());
        assertEquals(new BigDecimal("80"), reading.getValueSecondary());
    }

    @Test
    void create_bloodPressureWithoutSecondaryValue_throwsIllegalArgumentException() {
        BigDecimal primary = new BigDecimal("120");
        assertThrows(IllegalArgumentException.class, () ->
                VitalReading.create(PROFILE_ID, VitalType.BLOOD_PRESSURE, READING_DATE,
                        primary, null, "mmHg", null));
    }

    @Test
    void create_zeroValuePrimary_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                VitalReading.create(PROFILE_ID, VitalType.WEIGHT, READING_DATE,
                        BigDecimal.ZERO, null, "kg", null));
    }

    @Test
    void create_negativeValuePrimary_throwsIllegalArgumentException() {
        BigDecimal negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class, () ->
                VitalReading.create(PROFILE_ID, VitalType.WEIGHT, READING_DATE,
                        negative, null, "kg", null));
    }

    @Test
    void create_nullValuePrimary_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                VitalReading.create(PROFILE_ID, VitalType.WEIGHT, READING_DATE,
                        null, null, "kg", null));
    }

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
