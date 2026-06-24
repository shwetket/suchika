package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.household.domain.CalendarEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class CalendarEventResponse extends CalendarEventDto {

    @JsonProperty("conflicting_events")
    public List<CalendarEventConflictDto> conflictingEvents;

    public static CalendarEventResponse from(CalendarEvent event, List<CalendarEvent> conflicts) {
        CalendarEventResponse response = new CalendarEventResponse();
        response.id = event.getId();
        response.profileId = event.getProfileId();
        response.title = event.getTitle();
        response.eventType = event.getEventType() != null ? event.getEventType().name() : null;
        response.startDate = event.getStartDate();
        response.endDate = event.getEndDate();
        response.location = event.getLocation();
        response.notes = event.getNotes();
        response.createdAt = event.getCreatedAt();
        response.conflictingEvents = conflicts.stream()
                .map(CalendarEventConflictDto::from)
                .toList();
        return response;
    }
}
