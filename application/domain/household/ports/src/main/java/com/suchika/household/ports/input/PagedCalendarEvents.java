package com.suchika.household.ports.input;

import com.suchika.household.domain.CalendarEvent;

import java.util.List;

/**
 * Result of a paginated calendar event list query (Q54 pagination pass).
 * {@code totalCount} is the count across all pages matching the same filter,
 * not just {@code events.size()}.
 */
public record PagedCalendarEvents(List<CalendarEvent> events, long totalCount) {
}
