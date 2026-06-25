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

    CalendarEvent get(UUID id);

    CalendarEvent update(UUID id, String title, EventType eventType,
                         LocalDate startDate, LocalDate endDate,
                         String location, String notes);

    void delete(UUID id);

    List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate);
}
