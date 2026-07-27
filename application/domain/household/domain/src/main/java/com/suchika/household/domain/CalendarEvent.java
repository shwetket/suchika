package com.suchika.household.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class CalendarEvent {

    private final UUID id;
    private final UUID profileId;
    private final String title;
    private final EventType eventType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String location;
    private final String notes;
    private final Instant createdAt;

    private CalendarEvent(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.title = builder.title;
        this.eventType = builder.eventType;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.location = builder.location;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
    }

    public static CalendarEvent create(UUID profileId, String title, EventType eventType,
                                       LocalDate startDate, LocalDate endDate,
                                       String location, String notes) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("start_date must not be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("event_type must not be null");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end_date must not be before start_date");
        }
        return new Builder()
                .profileId(profileId)
                .title(title)
                .eventType(eventType)
                .startDate(startDate)
                .endDate(endDate)
                .location(location)
                .notes(notes)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private String title;
        private EventType eventType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String location;
        private String notes;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder eventType(EventType eventType) { this.eventType = eventType; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public CalendarEvent build() {
            return new CalendarEvent(this);
        }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getTitle() { return title; }
    public EventType getEventType() { return eventType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getLocation() { return location; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
