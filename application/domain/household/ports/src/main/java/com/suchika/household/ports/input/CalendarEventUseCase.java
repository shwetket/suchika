package com.suchika.household.ports.input;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarEventUseCase {

    CalendarEvent create(UUID profileId, String title, EventType eventType,
                         LocalDate startDate, LocalDate endDate,
                         String location, String notes);

    List<CalendarEvent> list(UUID profileId, EventType eventType,
                             LocalDate fromDate, LocalDate toDate);

    /**
     * Paginated variant of {@link #list} (Q54 pagination pass). {@code page} is
     * 0-indexed. Used by the HTTP list endpoint; {@link #list} stays as-is for
     * any caller that wants the full list.
     */
    PagedCalendarEvents listPaginated(UUID profileId, EventType eventType,
                                      LocalDate fromDate, LocalDate toDate, int page, int size);

    CalendarEvent get(UUID id);

    CalendarEvent update(UUID id, String title, EventType eventType,
                         LocalDate startDate, LocalDate endDate,
                         String location, String notes);

    void delete(UUID id);

    List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate);
}
