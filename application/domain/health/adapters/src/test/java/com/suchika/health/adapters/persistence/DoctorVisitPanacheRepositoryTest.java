package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.output.DoctorVisitRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DoctorVisitPanacheRepository, focused on the v0.6
 * UX item (date-range filter on doctor visit list).
 * Requires a running local PostgreSQL (app_db) with the profile schema seeded.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestTransaction
class DoctorVisitPanacheRepositoryTest {

    // Seeded in R__seed_health_test_data.sql — guaranteed to exist when profile service has run
    private static final java.util.UUID SEED_PROFILE_ID =
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Inject
    DoctorVisitRepository repository;

    @Test
    void findByProfileId_noDateFilter_returnsAllIncludingBothTestVisits() {
        repository.save(visit(LocalDate.of(2026, Month.MARCH, 1), "March visit"));
        repository.save(visit(LocalDate.of(2026, Month.APRIL, 1), "April visit"));

        List<DoctorVisit> results = repository.findByProfileId(SEED_PROFILE_ID, null, null);

        assertTrue(results.stream().anyMatch(v -> "March visit".equals(v.getDiagnosis())));
        assertTrue(results.stream().anyMatch(v -> "April visit".equals(v.getDiagnosis())));
    }

    @Test
    void findByProfileId_fromFilter_excludesOlderVisits() {
        repository.save(visit(LocalDate.of(2026, Month.JANUARY, 1), "January visit"));
        repository.save(visit(LocalDate.of(2026, Month.JUNE, 1), "June visit"));

        List<DoctorVisit> results = repository.findByProfileId(
                SEED_PROFILE_ID, LocalDate.of(2026, Month.JUNE, 1), null);

        assertTrue(results.stream().anyMatch(v -> "June visit".equals(v.getDiagnosis())));
        assertTrue(results.stream().noneMatch(v -> "January visit".equals(v.getDiagnosis())));
    }

    @Test
    void findByProfileId_toFilter_excludesNewerVisits() {
        repository.save(visit(LocalDate.of(2026, Month.FEBRUARY, 15), "February visit"));
        repository.save(visit(LocalDate.of(2026, Month.MARCH, 10), "March visit for to-filter"));

        List<DoctorVisit> results = repository.findByProfileId(
                SEED_PROFILE_ID, null, LocalDate.of(2026, Month.FEBRUARY, 28));

        assertTrue(results.stream().anyMatch(v -> "February visit".equals(v.getDiagnosis())));
        assertTrue(results.stream().noneMatch(v -> "March visit for to-filter".equals(v.getDiagnosis())));
    }

    @Test
    void findByProfileId_fromAndToFilter_returnsOnlyWithinRange() {
        repository.save(visit(LocalDate.of(2026, Month.JANUARY, 1), "Too early"));
        repository.save(visit(LocalDate.of(2026, Month.MAY, 15), "Within range"));
        repository.save(visit(LocalDate.of(2026, Month.DECEMBER, 1), "Too late"));

        List<DoctorVisit> results = repository.findByProfileId(
                SEED_PROFILE_ID, LocalDate.of(2026, Month.MAY, 1), LocalDate.of(2026, Month.MAY, 31));

        assertTrue(results.stream().anyMatch(v -> "Within range".equals(v.getDiagnosis())));
        assertTrue(results.stream().noneMatch(v -> "Too early".equals(v.getDiagnosis())));
        assertTrue(results.stream().noneMatch(v -> "Too late".equals(v.getDiagnosis())));
    }

    private DoctorVisit visit(LocalDate fromDate, String diagnosis) {
        return DoctorVisit.builder()
                .profileId(SEED_PROFILE_ID)
                .fromDate(fromDate)
                .visitedDoctor(false)
                .diagnosis(diagnosis)
                .build();
    }
}
