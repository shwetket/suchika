package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "calendar_event", schema = "household")
public class CalendarEventEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "title", nullable = false, length = 200)
    public String title;

    @Column(name = "event_type", nullable = false, length = 50)
    public String eventType;

    @Column(name = "start_date", nullable = false)
    public LocalDate startDate;

    @Column(name = "end_date")
    public LocalDate endDate;

    @Column(name = "location", length = 200)
    public String location;

    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;

    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (metadata == null) metadata = "{}";
    }

    public static CalendarEventEntity from(CalendarEvent event) {
        CalendarEventEntity e = new CalendarEventEntity();
        e.id = event.getId();
        e.profileId = event.getProfileId();
        e.title = event.getTitle();
        e.eventType = event.getEventType() != null ? event.getEventType().name() : null;
        e.startDate = event.getStartDate();
        e.endDate = event.getEndDate();
        e.location = event.getLocation();
        e.notes = event.getNotes();
        e.createdAt = event.getCreatedAt();
        return e;
    }

    public CalendarEvent toDomain() {
        return CalendarEvent.builder()
                .id(id)
                .profileId(profileId)
                .title(title)
                .eventType(eventType != null ? EventType.valueOf(eventType) : null)
                .startDate(startDate)
                .endDate(endDate)
                .location(location)
                .notes(notes)
                .createdAt(createdAt)
                .build();
    }
}
