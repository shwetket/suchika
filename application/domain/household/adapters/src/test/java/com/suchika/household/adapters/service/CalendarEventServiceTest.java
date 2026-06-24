package com.suchika.household.adapters.service;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.output.CalendarEventRepository;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarEventServiceTest {

    private static final LocalDate JAN_10 = LocalDate.of(2026, 1, 10);
    private static final LocalDate JAN_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate JAN_20 = LocalDate.of(2026, 1, 20);
    private static final LocalDate JAN_25 = LocalDate.of(2026, 1, 25);

    private StubCalendarEventRepository repository;
    private CalendarEventService service;

    @BeforeEach
    void setUp() {
        repository = new StubCalendarEventRepository();
        service = new CalendarEventService(repository);
    }

    @Test
    void create_happyPath_returnsEventWithId() {
        UUID profileId = UUID.randomUUID();
        CalendarEvent event = service.create(profileId, "School Sports Day", EventType.SCHOOL,
                JAN_10, JAN_10, "School Ground", null);

        assertNotNull(event.getId());
        assertEquals(profileId, event.getProfileId());
        assertEquals("School Sports Day", event.getTitle());
        assertEquals(EventType.SCHOOL, event.getEventType());
    }

    @Test
    void list_byProfileId_returnsOnlyThatProfilesEvents() {
        UUID profile1 = UUID.randomUUID();
        UUID profile2 = UUID.randomUUID();
        service.create(profile1, "Event A", EventType.PERSONAL, JAN_10, null, null, null);
        service.create(profile2, "Event B", EventType.FAMILY, JAN_15, null, null, null);

        List<CalendarEvent> results = service.list(profile1, null, null, null);

        assertEquals(1, results.size());
        assertEquals("Event A", results.get(0).getTitle());
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void get_found_returnsEvent() {
        UUID profileId = UUID.randomUUID();
        CalendarEvent created = service.create(profileId, "Doctor", EventType.MEDICAL,
                JAN_10, null, null, null);

        CalendarEvent found = service.get(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
                service.update(UUID.randomUUID(), "New Title", null, null, null, null, null));
    }

    @Test
    void update_patchesOnlyProvidedFields() {
        UUID profileId = UUID.randomUUID();
        CalendarEvent created = service.create(profileId, "Old Title", EventType.WORK,
                JAN_10, JAN_15, "Office", "old notes");

        CalendarEvent updated = service.update(created.getId(), "New Title", null, null, null, null, null);

        assertEquals("New Title", updated.getTitle());
        assertEquals(EventType.WORK, updated.getEventType());
        assertEquals(JAN_10, updated.getStartDate());
        assertEquals(JAN_15, updated.getEndDate());
        assertEquals("Office", updated.getLocation());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
    }

    @Test
    void delete_existing_removesEvent() {
        UUID profileId = UUID.randomUUID();
        CalendarEvent created = service.create(profileId, "To Delete", EventType.OTHER,
                JAN_10, null, null, null);

        service.delete(created.getId());

        assertThrows(NotFoundException.class, () -> service.get(created.getId()));
    }

    @Test
    void findConflicts_overlappingEvent_returnsConflict() {
        UUID profileId = UUID.randomUUID();
        // Create an event from Jan 10 to Jan 20
        service.create(profileId, "Long Trip", EventType.TRAVEL, JAN_10, JAN_20, null, null);

        // Check conflict for Jan 15 to Jan 25 (overlaps)
        List<CalendarEvent> conflicts = service.findConflicts(profileId, JAN_15, JAN_25);

        assertTrue(conflicts.stream().anyMatch(e -> "Long Trip".equals(e.getTitle())));
    }

    @Test
    void findConflicts_nonOverlappingEvent_returnsEmpty() {
        UUID profileId = UUID.randomUUID();
        // Create Jan 10-15
        service.create(profileId, "Early Event", EventType.PERSONAL, JAN_10, JAN_15, null, null);

        // Check for Jan 20-25 (no overlap)
        List<CalendarEvent> conflicts = service.findConflicts(profileId, JAN_20, JAN_25);

        assertTrue(conflicts.isEmpty());
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubCalendarEventRepository implements CalendarEventRepository {

        private final Map<UUID, CalendarEvent> store = new LinkedHashMap<>();

        @Override
        public CalendarEvent save(CalendarEvent event) {
            UUID id = event.getId() != null ? event.getId() : UUID.randomUUID();
            CalendarEvent stored = CalendarEvent.builder()
                    .id(id)
                    .profileId(event.getProfileId())
                    .title(event.getTitle())
                    .eventType(event.getEventType())
                    .startDate(event.getStartDate())
                    .endDate(event.getEndDate())
                    .location(event.getLocation())
                    .notes(event.getNotes())
                    .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.EPOCH)
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<CalendarEvent> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<CalendarEvent> findByProfileId(UUID profileId, EventType eventType,
                                                   LocalDate fromDate, LocalDate toDate) {
            return store.values().stream()
                    .filter(e -> e.getProfileId().equals(profileId))
                    .filter(e -> eventType == null || e.getEventType() == eventType)
                    .filter(e -> fromDate == null || !e.getStartDate().isBefore(fromDate))
                    .filter(e -> toDate == null || !e.getStartDate().isAfter(toDate))
                    .toList();
        }

        @Override
        public List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate) {
            LocalDate effectiveEnd = endDate != null ? endDate : startDate;
            return store.values().stream()
                    .filter(e -> e.getProfileId().equals(profileId))
                    .filter(e -> {
                        LocalDate eEnd = e.getEndDate() != null ? e.getEndDate() : e.getStartDate();
                        return !(eEnd.isBefore(startDate) || e.getStartDate().isAfter(effectiveEnd));
                    })
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }
    }
}
