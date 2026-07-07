package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.output.DoctorVisitRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
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
@TestProfile(DoctorVisitPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class DoctorVisitPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

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

    // ---- pre-v1.0 pagination pass (Q54) ----

    @Test
    void findByProfileId_paginated_returnsRequestedPageOrderedNewestFirst() {
        repository.save(visit(LocalDate.of(2026, Month.SEPTEMBER, 1), "Sept 1 paged visit"));
        repository.save(visit(LocalDate.of(2026, Month.SEPTEMBER, 2), "Sept 2 paged visit"));
        repository.save(visit(LocalDate.of(2026, Month.SEPTEMBER, 3), "Sept 3 paged visit"));

        List<DoctorVisit> firstPage = repository.findByProfileId(SEED_PROFILE_ID,
                LocalDate.of(2026, Month.SEPTEMBER, 1), LocalDate.of(2026, Month.SEPTEMBER, 3), 0, 2);
        List<DoctorVisit> secondPage = repository.findByProfileId(SEED_PROFILE_ID,
                LocalDate.of(2026, Month.SEPTEMBER, 1), LocalDate.of(2026, Month.SEPTEMBER, 3), 1, 2);

        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());
        assertEquals("Sept 3 paged visit", firstPage.get(0).getDiagnosis());
        assertEquals("Sept 2 paged visit", firstPage.get(1).getDiagnosis());
        assertEquals("Sept 1 paged visit", secondPage.get(0).getDiagnosis());
    }

    @Test
    void countByProfileId_returnsTotalMatchingFilterIgnoringPage() {
        repository.save(visit(LocalDate.of(2026, Month.OCTOBER, 1), "Oct 1 count visit"));
        repository.save(visit(LocalDate.of(2026, Month.OCTOBER, 2), "Oct 2 count visit"));
        repository.save(visit(LocalDate.of(2026, Month.OCTOBER, 3), "Oct 3 count visit"));

        long count = repository.countByProfileId(SEED_PROFILE_ID,
                LocalDate.of(2026, Month.OCTOBER, 1), LocalDate.of(2026, Month.OCTOBER, 3));

        assertEquals(3, count);
    }

    @Test
    void countByProfileId_appliesSameDateFilterAsFindByProfileId() {
        repository.save(visit(LocalDate.of(2026, Month.NOVEMBER, 1), "Nov 1 visit"));
        repository.save(visit(LocalDate.of(2026, Month.NOVEMBER, 15), "Nov 15 visit"));

        long count = repository.countByProfileId(SEED_PROFILE_ID,
                LocalDate.of(2026, Month.NOVEMBER, 10), LocalDate.of(2026, Month.NOVEMBER, 30));

        assertEquals(1, count);
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
