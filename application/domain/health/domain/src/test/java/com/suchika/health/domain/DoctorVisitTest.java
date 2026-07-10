package com.suchika.health.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorVisitTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, Month.JULY, 5);
    private static final LocalDate TOMORROW = LocalDate.of(2026, Month.JULY, 6);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, Month.JULY, 4);

    @Test
    void create_visitedDoctor_happyPath_returnsVisitWithAllFields() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, TOMORROW, true, "Dr. Sharma",
                new DoctorVisit.VisitDetails("Apollo", "General Medicine",
                        "Fever", "Viral infection", "Rest advised", TOMORROW));

        assertNotNull(visit);
        assertEquals(PROFILE_ID, visit.getProfileId());
        assertEquals(TODAY, visit.getFromDate());
        assertEquals(TOMORROW, visit.getToDate());
        assertTrue(visit.isVisitedDoctor());
        assertEquals("Dr. Sharma", visit.getDoctorName());
        assertEquals("Apollo", visit.getHospitalName());
    }

    @Test
    void create_illnessWithoutVisit_doctorNameNotRequired() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, null, false, null,
                new DoctorVisit.VisitDetails(null, null, "Fever", null, null, null));

        assertFalse(visit.isVisitedDoctor());
        assertNull(visit.getDoctorName());
    }

    @Test
    void create_singleDayVisit_toDateNull_succeeds() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, null, true, "Dr. Sharma",
                DoctorVisit.VisitDetails.empty());

        assertNull(visit.getToDate());
    }

    @Test
    void create_toDateEqualToFromDate_succeeds() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, TODAY, true, "Dr. Sharma",
                DoctorVisit.VisitDetails.empty());

        assertEquals(TODAY, visit.getFromDate());
        assertEquals(TODAY, visit.getToDate());
    }

    @Test
    void create_toDateBeforeFromDate_throwsIllegalArgumentException() {
        DoctorVisit.VisitDetails details = DoctorVisit.VisitDetails.empty();
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, YESTERDAY, false, null, details));
    }

    @Test
    void create_visitedDoctorTrueWithNullDoctorName_throwsIllegalArgumentException() {
        DoctorVisit.VisitDetails details = DoctorVisit.VisitDetails.empty();
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, null, true, null, details));
    }

    @Test
    void create_visitedDoctorTrueWithBlankDoctorName_throwsIllegalArgumentException() {
        DoctorVisit.VisitDetails details = DoctorVisit.VisitDetails.empty();
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, null, true, "   ", details));
    }

    @Test
    void create_populatesAllOptionalDetailFields_getterReturnEachValue() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, TOMORROW, true, "Dr. Sharma",
                new DoctorVisit.VisitDetails("Apollo", "Cardiology",
                        "Chest pain", "Angina", "Follow up in 2 weeks", TOMORROW));

        assertEquals("Cardiology", visit.getSpeciality());
        assertEquals("Chest pain", visit.getSymptoms());
        assertEquals("Angina", visit.getDiagnosis());
        assertEquals("Follow up in 2 weeks", visit.getNotes());
        assertEquals(TOMORROW, visit.getFollowUpDate());
    }

    @Test
    void create_nullDetails_defaultsToEmptyDetails() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, null, false, null, null);

        assertNull(visit.getHospitalName());
        assertNull(visit.getSpeciality());
        assertNull(visit.getSymptoms());
        assertNull(visit.getDiagnosis());
        assertNull(visit.getNotes());
        assertNull(visit.getFollowUpDate());
    }

    @Test
    void create_fromDateNull_toDateProvided_succeeds() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, null, TOMORROW, false, null,
                DoctorVisit.VisitDetails.empty());

        assertNull(visit.getFromDate());
        assertEquals(TOMORROW, visit.getToDate());
    }

    @Test
    void noArgsConstructor_createsEmptyInstanceWithNullDefaults() {
        DoctorVisit visit = new DoctorVisit();

        assertNull(visit.getId());
        assertNull(visit.getProfileId());
        assertNull(visit.getFromDate());
        assertNull(visit.getToDate());
        assertFalse(visit.isVisitedDoctor());
        assertNull(visit.getDoctorName());
        assertNull(visit.getCreatedAt());
    }

    @Test
    void builder_setsIdAndCreatedAt_returnsVisitWithExplicitValues() {
        java.time.Instant createdAt = java.time.Instant.parse("2026-07-05T10:00:00Z");
        java.util.UUID id = UUID.randomUUID();

        DoctorVisit visit = DoctorVisit.builder()
                .id(id)
                .profileId(PROFILE_ID)
                .fromDate(TODAY)
                .visitedDoctor(false)
                .createdAt(createdAt)
                .build();

        assertEquals(id, visit.getId());
        assertEquals(createdAt, visit.getCreatedAt());
    }
}
