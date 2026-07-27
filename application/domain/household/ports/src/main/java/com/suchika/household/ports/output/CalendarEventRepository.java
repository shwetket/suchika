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

    /**
     * Paginated variant of {@link #findByProfileId} (Q54 pagination pass). {@code page}
     * is 0-indexed. Same filter predicate as the unpaginated method; use
     * {@link #countByProfileId} for the total matching count.
     */
    List<CalendarEvent> findByProfileId(UUID profileId, EventType eventType,
                                        LocalDate fromDate, LocalDate toDate, int page, int size);

    /**
     * Total count of events matching the same filter predicate as
     * {@link #findByProfileId}, ignoring pagination — used to compute total pages.
     */
    long countByProfileId(UUID profileId, EventType eventType, LocalDate fromDate, LocalDate toDate);

    List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
