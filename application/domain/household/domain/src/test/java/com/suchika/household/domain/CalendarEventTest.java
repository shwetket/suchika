package com.suchika.household.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalendarEventTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 24);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 6, 25);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 6, 23);

    @Test
    void create_happyPath_returnsEventWithAllFields() {
        CalendarEvent event = CalendarEvent.create(
                PROFILE_ID, "Family Dinner", EventType.FAMILY,
                TODAY, TOMORROW, "Home", "Annual gathering");

        assertNotNull(event);
        assertEquals(PROFILE_ID, event.getProfileId());
        assertEquals("Family Dinner", event.getTitle());
        assertEquals(EventType.FAMILY, event.getEventType());
        assertEquals(TODAY, event.getStartDate());
        assertEquals(TOMORROW, event.getEndDate());
        assertEquals("Home", event.getLocation());
        assertEquals("Annual gathering", event.getNotes());
    }

    @Test
    void create_singleDayEvent_endDateNull_succeeds() {
        CalendarEvent event = CalendarEvent.create(
                PROFILE_ID, "Dentist", EventType.MEDICAL,
                TODAY, null, null, null);

        assertNotNull(event);
        assertNull(event.getEndDate());
    }

    @Test
    void create_blankTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                CalendarEvent.create(PROFILE_ID, "  ", EventType.PERSONAL,
                        TODAY, null, null, null));
    }

    @Test
    void create_nullTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                CalendarEvent.create(PROFILE_ID, null, EventType.PERSONAL,
                        TODAY, null, null, null));
    }

    @Test
    void create_nullStartDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                CalendarEvent.create(PROFILE_ID, "Event", EventType.PERSONAL,
                        null, null, null, null));
    }

    @Test
    void create_endDateBeforeStartDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                CalendarEvent.create(PROFILE_ID, "Bad event", EventType.TRAVEL,
                        TODAY, YESTERDAY, null, null));
    }

    @Test
    void create_endDateEqualToStartDate_succeeds() {
        CalendarEvent event = CalendarEvent.create(
                PROFILE_ID, "Same day", EventType.WORK,
                TODAY, TODAY, null, null);

        assertEquals(TODAY, event.getStartDate());
        assertEquals(TODAY, event.getEndDate());
    }

    @Test
    void create_nullEventType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                CalendarEvent.create(PROFILE_ID, "Event", null,
                        TODAY, null, null, null));
    }
}
