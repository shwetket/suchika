package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.output.VitalReadingRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for VitalReadingPanacheRepository.
 * Requires a running local PostgreSQL (app_db) with the profile schema seeded.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestTransaction
class VitalReadingPanacheRepositoryTest {

    // Seeded in R__seed_health_test_data.sql — guaranteed to exist when profile service has run
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Inject
    VitalReadingRepository repository;

    @Test
    void save_andFindById_roundTrip() {
        VitalReading saved = repository.save(reading(LocalDate.of(2026, Month.JANUARY, 1), VitalType.WEIGHT,
                new BigDecimal("72.50"), null, "kg"));

        assertNotNull(saved.getId());
        assertEquals(SEED_PROFILE_ID, saved.getProfileId());
        assertEquals(VitalType.WEIGHT, saved.getVitalType());
        assertEquals(LocalDate.of(2026, Month.JANUARY, 1), saved.getReadingDate());
        assertEquals(0, new BigDecimal("72.50").compareTo(saved.getValuePrimary()));

        Optional<VitalReading> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("72.50").compareTo(found.get().getValuePrimary()));
    }

    @Test
    void findByProfileId_orderedByDateDesc() {
        repository.save(reading(LocalDate.of(2026, Month.JANUARY, 1), VitalType.WEIGHT, new BigDecimal("70.0"), null, "kg"));
        repository.save(reading(LocalDate.of(2026, Month.MARCH, 1), VitalType.WEIGHT, new BigDecimal("72.0"), null, "kg"));
        repository.save(reading(LocalDate.of(2026, Month.FEBRUARY, 1), VitalType.WEIGHT, new BigDecimal("71.0"), null, "kg"));

        List<VitalReading> results = repository.findByProfileId(SEED_PROFILE_ID, VitalType.WEIGHT);

        // Most recent date first — the March reading should be at the top of test-added rows
        assertTrue(results.stream()
                .anyMatch(r -> r.getReadingDate().equals(LocalDate.of(2026, Month.MARCH, 1))));

        // Verify ordering (all test-added rows have dates in 2026)
        List<LocalDate> dates = results.stream()
                .filter(r -> r.getReadingDate().getYear() == 2026)
                .map(VitalReading::getReadingDate)
                .toList();
        for (int i = 0; i < dates.size() - 1; i++) {
            assertFalse(dates.get(i).isBefore(dates.get(i + 1)),
                    "Expected DESC order but got " + dates.get(i) + " before " + dates.get(i + 1));
        }
    }

    @Test
    void findByProfileId_filtersByVitalType() {
        repository.save(reading(LocalDate.of(2026, Month.APRIL, 1), VitalType.WEIGHT, new BigDecimal("70.0"), null, "kg"));
        repository.save(reading(LocalDate.of(2026, Month.APRIL, 2), VitalType.BLOOD_PRESSURE, new BigDecimal("120.0"), new BigDecimal("80.0"), "mmHg"));

        List<VitalReading> bpOnly = repository.findByProfileId(SEED_PROFILE_ID, VitalType.BLOOD_PRESSURE);

        assertTrue(bpOnly.stream().allMatch(r -> r.getVitalType() == VitalType.BLOOD_PRESSURE));
        assertTrue(bpOnly.stream().anyMatch(r -> r.getReadingDate().equals(LocalDate.of(2026, Month.APRIL, 2))));
    }

    @Test
    void findByProfileId_scopedToProfile_doesNotReturnOtherProfiles() {
        // Note: using a random UUID won't have a valid profile FK — profile_id NOT NULL REFERENCES profile.profile
        // So we just verify that our known profile's data is not leaked to unrelated queries
        UUID unrelatedProfile = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000099");

        repository.save(reading(LocalDate.of(2026, Month.MAY, 1), VitalType.WEIGHT, new BigDecimal("70.0"), null, "kg"));

        List<VitalReading> results = repository.findByProfileId(unrelatedProfile, null);

        // unrelated profile has no readings (seed data uses SEED_PROFILE_ID)
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteById_removesReading() {
        VitalReading saved = repository.save(reading(LocalDate.of(2026, Month.JUNE, 1), VitalType.HEART_RATE,
                new BigDecimal("72"), null, "bpm"));
        UUID id = saved.getId();
        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void existsById_returnsFalseForUnknownId() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void save_withSecondaryValue_roundTrips() {
        VitalReading saved = repository.save(reading(LocalDate.of(2026, Month.JULY, 1), VitalType.BLOOD_PRESSURE,
                new BigDecimal("118.0"), new BigDecimal("76.0"), "mmHg"));

        Optional<VitalReading> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("118.0").compareTo(found.get().getValuePrimary()));
        assertEquals(0, new BigDecimal("76.0").compareTo(found.get().getValueSecondary()));
    }

    private VitalReading reading(LocalDate date, VitalType type, BigDecimal primary, BigDecimal secondary, String unit) {
        return VitalReading.builder()
                .profileId(SEED_PROFILE_ID)
                .vitalType(type)
                .readingDate(date)
                .valuePrimary(primary)
                .valueSecondary(secondary)
                .unit(unit)
                .build();
    }
}
