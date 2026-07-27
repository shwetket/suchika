package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;

@RegisterForReflection
public class UpdateCalendarEventRequest {

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
}
