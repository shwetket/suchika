package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.household.domain.CalendarEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalendarEventConflictDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("title")
    public String title;

    @JsonProperty("start_date")
    public LocalDate startDate;

    @JsonProperty("end_date")
    public LocalDate endDate;

    public static CalendarEventConflictDto from(CalendarEvent event) {
        CalendarEventConflictDto dto = new CalendarEventConflictDto();
        dto.id = event.getId();
        dto.title = event.getTitle();
        dto.startDate = event.getStartDate();
        dto.endDate = event.getEndDate();
        return dto;
    }
}
