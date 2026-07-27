package com.suchika.household.adapters.service;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.input.CalendarEventUseCase;
import com.suchika.household.ports.input.PagedCalendarEvents;
import com.suchika.household.ports.output.CalendarEventRepository;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CalendarEventService implements CalendarEventUseCase {

    private static final String EVENT_NOT_FOUND = "Calendar event not found: ";

    private final CalendarEventRepository repository;

    public CalendarEventService(CalendarEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CalendarEvent create(UUID profileId, String title, EventType eventType,
                                LocalDate startDate, LocalDate endDate,
                                String location, String notes) {
        CalendarEvent event = CalendarEvent.create(profileId, title, eventType,
                startDate, endDate, location, notes);
        CalendarEvent saved = repository.save(event);
        AppLogger.info("Calendar event created: %s for profile: %s", saved.getId(), profileId);
        return saved;
    }

    @Override
    public List<CalendarEvent> list(UUID profileId, EventType eventType,
                                    LocalDate fromDate, LocalDate toDate) {
        return repository.findByProfileId(profileId, eventType, fromDate, toDate);
    }

    @Override
    public PagedCalendarEvents listPaginated(UUID profileId, EventType eventType,
                                             LocalDate fromDate, LocalDate toDate, int page, int size) {
        List<CalendarEvent> events = repository.findByProfileId(profileId, eventType, fromDate, toDate, page, size);
        long totalCount = repository.countByProfileId(profileId, eventType, fromDate, toDate);
        return new PagedCalendarEvents(events, totalCount);
    }

    @Override
    public CalendarEvent get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND + id));
    }

    @Override
    @Transactional
    public CalendarEvent update(UUID id, String title, EventType eventType,
                                LocalDate startDate, LocalDate endDate,
                                String location, String notes) {
        CalendarEvent existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(EVENT_NOT_FOUND + id));

        String newTitle = title != null ? title : existing.getTitle();
        EventType newType = eventType != null ? eventType : existing.getEventType();
        LocalDate newStart = startDate != null ? startDate : existing.getStartDate();
        LocalDate newEnd = endDate != null ? endDate : existing.getEndDate();
        String newLocation = location != null ? location : existing.getLocation();
        String newNotes = notes != null ? notes : existing.getNotes();

        CalendarEvent updated = CalendarEvent.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .title(newTitle)
                .eventType(newType)
                .startDate(newStart)
                .endDate(newEnd)
                .location(newLocation)
                .notes(newNotes)
                .createdAt(existing.getCreatedAt())
                .build();

        return repository.save(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(EVENT_NOT_FOUND + id);
        }
        repository.deleteById(id);
        AppLogger.info("Calendar event deleted: %s", id);
    }

    @Override
    public List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate) {
        return repository.findConflicts(profileId, startDate, endDate);
    }
}
