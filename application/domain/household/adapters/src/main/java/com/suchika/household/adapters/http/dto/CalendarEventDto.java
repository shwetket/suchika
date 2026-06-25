package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.household.domain.CalendarEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalendarEventDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("title")
    public String title;

    @JsonProperty("event_type")
    public String eventType;

    @JsonProperty("start_date")
    public LocalDate startDate;

    @JsonProperty("end_date")
    public LocalDate endDate;

    @JsonProperty("location")
    public String location;

    @JsonProperty("notes")
    public String notes;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static CalendarEventDto from(CalendarEvent event) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.id = event.getId();
        dto.profileId = event.getProfileId();
        dto.title = event.getTitle();
        dto.eventType = event.getEventType() != null ? event.getEventType().name() : null;
        dto.startDate = event.getStartDate();
        dto.endDate = event.getEndDate();
        dto.location = event.getLocation();
        dto.notes = event.getNotes();
        dto.createdAt = event.getCreatedAt();
        return dto;
    }
}
