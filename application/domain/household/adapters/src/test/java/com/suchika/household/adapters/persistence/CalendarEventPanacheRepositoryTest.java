package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.output.CalendarEventRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CalendarEventPanacheRepository.
 * Requires a running local PostgreSQL (app_db) with household schema and test seed data.
 * Profile seed (R__seed_profile_test_data.sql) must have run before these tests.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestProfile(CalendarEventPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class CalendarEventPanacheRepositoryTest {

    /**
     * Activates the 'integration-test' Quarkus config profile which re-enables
     * the datasource. The default 'test' profile disables the DB for @InjectMock tests.
     */
    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }

    // Seeded by R__seed_profile_test_data.sql via profile domain migrations
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // A second distinct UUID for profile-isolation tests — has no rows seeded for it
    private static final UUID OTHER_PROFILE_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000099");

    @Inject
    CalendarEventRepository repository;

    // --- save + findById ---

    @Test
    void save_andFindById_roundTrip() {
        CalendarEvent saved = repository.save(event("Team Standup", EventType.WORK,
                LocalDate.of(2026, Month.AUGUST, 1), LocalDate.of(2026, Month.AUGUST, 1)));

        assertNotNull(saved.getId());
        assertEquals(SEED_PROFILE_ID, saved.getProfileId());
        assertEquals("Team Standup", saved.getTitle());
        assertEquals(EventType.WORK, saved.getEventType());
        assertEquals(LocalDate.of(2026, Month.AUGUST, 1), saved.getStartDate());
        assertNotNull(saved.getCreatedAt());

        Optional<CalendarEvent> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Team Standup", found.get().getTitle());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    // --- findByProfileId filters ---

    @Test
    void findByProfileId_noFilters_returnsAllForProfile() {
        repository.save(event("Event A", EventType.WORK,
                LocalDate.of(2026, Month.SEPTEMBER, 1), null));
        repository.save(event("Event B", EventType.FAMILY,
                LocalDate.of(2026, Month.SEPTEMBER, 5), null));

        List<CalendarEvent> results = repository.findByProfileId(SEED_PROFILE_ID, null, null, null);

        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(e -> "Event A".equals(e.getTitle())));
        assertTrue(results.stream().anyMatch(e -> "Event B".equals(e.getTitle())));
    }

    @Test
    void findByProfileId_eventTypeFilter_returnsOnlyMatchingType() {
        repository.save(event("Work Meeting", EventType.WORK,
                LocalDate.of(2026, Month.OCTOBER, 1), null));
        repository.save(event("Family Picnic", EventType.FAMILY,
                LocalDate.of(2026, Month.OCTOBER, 2), null));

        List<CalendarEvent> workOnly = repository.findByProfileId(SEED_PROFILE_ID, EventType.WORK, null, null);

        assertTrue(workOnly.stream().allMatch(e -> e.getEventType() == EventType.WORK));
        assertTrue(workOnly.stream().anyMatch(e -> "Work Meeting".equals(e.getTitle())));
        assertTrue(workOnly.stream().noneMatch(e -> "Family Picnic".equals(e.getTitle())));
    }

    @Test
    void findByProfileId_fromDateFilter_excludesOlderEvents() {
        repository.save(event("Old Event", EventType.PERSONAL,
                LocalDate.of(2025, Month.JANUARY, 1), null));
        repository.save(event("New Event", EventType.PERSONAL,
                LocalDate.of(2026, Month.NOVEMBER, 1), null));

        List<CalendarEvent> results = repository.findByProfileId(
                SEED_PROFILE_ID, null, LocalDate.of(2026, Month.JUNE, 1), null);

        assertTrue(results.stream().anyMatch(e -> "New Event".equals(e.getTitle())));
        assertTrue(results.stream().noneMatch(e -> "Old Event".equals(e.getTitle())));
    }

    @Test
    void findByProfileId_toDateFilter_excludesNewerEvents() {
        repository.save(event("January Event", EventType.PERSONAL,
                LocalDate.of(2026, Month.JANUARY, 10), null));
        repository.save(event("December Event", EventType.PERSONAL,
                LocalDate.of(2026, Month.DECEMBER, 10), null));

        List<CalendarEvent> results = repository.findByProfileId(
                SEED_PROFILE_ID, null, null, LocalDate.of(2026, Month.JUNE, 30));

        assertTrue(results.stream().anyMatch(e -> "January Event".equals(e.getTitle())));
        assertTrue(results.stream().noneMatch(e -> "December Event".equals(e.getTitle())));
    }

    @Test
    void findByProfileId_orderedByStartDateDesc() {
        repository.save(event("Oldest", EventType.WORK, LocalDate.of(2026, Month.JANUARY, 1), null));
        repository.save(event("Middle", EventType.WORK, LocalDate.of(2026, Month.JUNE, 15), null));
        repository.save(event("Newest", EventType.WORK, LocalDate.of(2026, Month.DECEMBER, 31), null));

        List<CalendarEvent> results = repository.findByProfileId(SEED_PROFILE_ID, EventType.WORK, null, null);

        List<LocalDate> dates = results.stream()
                .map(CalendarEvent::getStartDate)
                .toList();
        for (int i = 0; i < dates.size() - 1; i++) {
            assertFalse(dates.get(i).isBefore(dates.get(i + 1)),
                    "Expected DESC order but found " + dates.get(i) + " before " + dates.get(i + 1));
        }
    }

    // --- profile-scoped isolation ---

    @Test
    void findByProfileId_doesNotLeakAcrossProfiles() {
        repository.save(event("Profile A Event", EventType.WORK,
                LocalDate.of(2026, Month.AUGUST, 10), null));

        // OTHER_PROFILE_ID has no FK row in profile.profile — querying it returns empty
        List<CalendarEvent> otherResults = repository.findByProfileId(OTHER_PROFILE_ID, null, null, null);

        assertTrue(otherResults.isEmpty());
    }

    // --- conflict detection ---

    @Test
    void findConflicts_overlappingDateRange_returnsConflict() {
        repository.save(event("Existing Event", EventType.FAMILY,
                LocalDate.of(2026, Month.AUGUST, 5), LocalDate.of(2026, Month.AUGUST, 10)));

        List<CalendarEvent> conflicts = repository.findConflicts(
                SEED_PROFILE_ID,
                LocalDate.of(2026, Month.AUGUST, 7),
                LocalDate.of(2026, Month.AUGUST, 12));

        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(e -> "Existing Event".equals(e.getTitle())));
    }

    @Test
    void findConflicts_noOverlap_returnsEmpty() {
        repository.save(event("Past Event", EventType.FAMILY,
                LocalDate.of(2026, Month.AUGUST, 1), LocalDate.of(2026, Month.AUGUST, 3)));

        List<CalendarEvent> conflicts = repository.findConflicts(
                SEED_PROFILE_ID,
                LocalDate.of(2026, Month.AUGUST, 10),
                LocalDate.of(2026, Month.AUGUST, 15));

        // May contain seed data conflicts but not the specific past event
        assertTrue(conflicts.stream().noneMatch(e -> "Past Event".equals(e.getTitle())));
    }

    @Test
    void findConflicts_singleDayEvent_noEndDate_usesStartDateAsEnd() {
        repository.save(event("Single Day", EventType.MEDICAL,
                LocalDate.of(2026, Month.SEPTEMBER, 20), null));

        List<CalendarEvent> conflicts = repository.findConflicts(
                SEED_PROFILE_ID,
                LocalDate.of(2026, Month.SEPTEMBER, 20),
                null);

        assertTrue(conflicts.stream().anyMatch(e -> "Single Day".equals(e.getTitle())));
    }

    // --- save update ---

    @Test
    void save_update_persistsChanges() {
        CalendarEvent saved = repository.save(event("Original Title", EventType.WORK,
                LocalDate.of(2026, Month.NOVEMBER, 1), null));

        CalendarEvent updated = CalendarEvent.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .title("Updated Title")
                .eventType(EventType.PERSONAL)
                .startDate(LocalDate.of(2026, Month.NOVEMBER, 2))
                .createdAt(saved.getCreatedAt())
                .build();

        CalendarEvent result = repository.save(updated);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(EventType.PERSONAL, result.getEventType());
        assertEquals(LocalDate.of(2026, Month.NOVEMBER, 2), result.getStartDate());
    }

    // --- delete ---

    @Test
    void deleteById_removesEvent() {
        CalendarEvent saved = repository.save(event("To Delete", EventType.OTHER,
                LocalDate.of(2026, Month.DECEMBER, 1), null));
        UUID id = saved.getId();
        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        assertTrue(repository.findById(id).isEmpty());
    }

    // --- existsById ---

    @Test
    void existsById_falseForUnknownId() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void existsById_trueAfterSave() {
        CalendarEvent saved = repository.save(event("Exists Check", EventType.SCHOOL,
                LocalDate.of(2026, Month.AUGUST, 20), null));
        assertTrue(repository.existsById(saved.getId()));
    }

    // --- validation guard (DB constraint) ---

    @Test
    void save_endDateBeforeStartDate_throwsConstraintViolation() {
        // DB CHECK constraint: chk_event_dates — end_date >= start_date
        CalendarEvent invalid = CalendarEvent.builder()
                .profileId(SEED_PROFILE_ID)
                .title("Bad Dates")
                .eventType(EventType.WORK)
                .startDate(LocalDate.of(2026, Month.AUGUST, 15))
                .endDate(LocalDate.of(2026, Month.AUGUST, 10))
                .build();

        assertThrows(Exception.class, () -> repository.save(invalid),
                "Expected constraint violation for end_date < start_date");
    }

    // ---- helper ----

    private CalendarEvent event(String title, EventType type, LocalDate startDate, LocalDate endDate) {
        return CalendarEvent.builder()
                .profileId(SEED_PROFILE_ID)
                .title(title)
                .eventType(type)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
