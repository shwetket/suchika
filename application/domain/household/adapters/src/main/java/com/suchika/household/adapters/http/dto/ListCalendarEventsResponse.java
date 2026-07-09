package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListCalendarEventsResponse {

    @JsonProperty("calendar_events")
    public List<CalendarEventDto> calendarEvents;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListCalendarEventsResponse(List<CalendarEventDto> calendarEvents) {
        this.calendarEvents = calendarEvents;
        this.totalSize = calendarEvents.size();
    }

    /**
     * Paginated variant (Q54 pagination pass) — totalSize is the grand total matching
     * the filter across all pages, not just calendarEvents.size().
     */
    public ListCalendarEventsResponse(List<CalendarEventDto> calendarEvents, long totalSize, int page, int size) {
        this.calendarEvents = calendarEvents;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
