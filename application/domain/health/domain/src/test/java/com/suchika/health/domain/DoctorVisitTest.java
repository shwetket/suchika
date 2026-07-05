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
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, TOMORROW, true,
                "Dr. Sharma", "Apollo", "General Medicine",
                "Fever", "Viral infection", "Rest advised", TOMORROW);

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
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, null, false,
                null, null, null, "Fever", null, null, null);

        assertFalse(visit.isVisitedDoctor());
        assertNull(visit.getDoctorName());
    }

    @Test
    void create_singleDayVisit_toDateNull_succeeds() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, null, true,
                "Dr. Sharma", null, null, null, null, null, null);

        assertNull(visit.getToDate());
    }

    @Test
    void create_toDateEqualToFromDate_succeeds() {
        DoctorVisit visit = DoctorVisit.create(PROFILE_ID, TODAY, TODAY, true,
                "Dr. Sharma", null, null, null, null, null, null);

        assertEquals(TODAY, visit.getFromDate());
        assertEquals(TODAY, visit.getToDate());
    }

    @Test
    void create_toDateBeforeFromDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, YESTERDAY, false,
                        null, null, null, null, null, null, null));
    }

    @Test
    void create_visitedDoctorTrueWithNullDoctorName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, null, true,
                        null, null, null, null, null, null, null));
    }

    @Test
    void create_visitedDoctorTrueWithBlankDoctorName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                DoctorVisit.create(PROFILE_ID, TODAY, null, true,
                        "   ", null, null, null, null, null, null));
    }
}
