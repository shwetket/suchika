package com.suchika.household.ports.output;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository {

    CalendarEvent save(CalendarEvent event);

    Optional<CalendarEvent> findById(UUID id);

    List<CalendarEvent> findByProfileId(UUID profileId, EventType eventType,
                                        LocalDate fromDate, LocalDate toDate);

    List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
