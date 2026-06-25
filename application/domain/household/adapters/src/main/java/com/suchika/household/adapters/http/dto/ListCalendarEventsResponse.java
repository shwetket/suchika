package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListCalendarEventsResponse {

    @JsonProperty("calendar_events")
    public List<CalendarEventDto> calendarEvents;

    @JsonProperty("total_size")
    public int totalSize;

    public ListCalendarEventsResponse(List<CalendarEventDto> calendarEvents) {
        this.calendarEvents = calendarEvents;
        this.totalSize = calendarEvents.size();
    }
}
