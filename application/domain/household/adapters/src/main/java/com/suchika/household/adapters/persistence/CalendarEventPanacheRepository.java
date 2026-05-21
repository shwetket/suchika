package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.output.CalendarEventRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CalendarEventPanacheRepository implements CalendarEventRepository {

    private final CalendarEventDao dao;

    public CalendarEventPanacheRepository(CalendarEventDao dao) {
        this.dao = dao;
    }

    @Override
    public CalendarEvent save(CalendarEvent event) {
        CalendarEventEntity entity = CalendarEventEntity.from(event);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<CalendarEvent> findById(UUID id) {
        return dao.findByIdOptional(id).map(CalendarEventEntity::toDomain);
    }

    @Override
    public List<CalendarEvent> findByProfileId(UUID profileId, EventType eventType,
                                               LocalDate fromDate, LocalDate toDate) {
        String query = buildListQuery(eventType, fromDate, toDate);
        List<Object> params = buildListParams(profileId, eventType, fromDate, toDate);
        return dao.find(query, params.toArray())
                .stream()
                .map(CalendarEventEntity::toDomain)
                .toList();
    }

    @Override
    public List<CalendarEvent> findConflicts(UUID profileId, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEnd = endDate != null ? endDate : startDate;
        return dao.find(
                "profileId = ?1 AND NOT (COALESCE(endDate, startDate) < ?2 OR startDate > ?3)",
                profileId, startDate, effectiveEnd
        ).stream().map(CalendarEventEntity::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        dao.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.count("id = ?1", id) > 0;
    }

    private String buildListQuery(EventType eventType, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sb = new StringBuilder("profileId = ?1");
        int idx = 2;
        if (eventType != null) { sb.append(" AND eventType = ?").append(idx++); }
        if (fromDate != null) { sb.append(" AND startDate >= ?").append(idx++); }
        if (toDate != null) { sb.append(" AND startDate <= ?").append(idx); }
        sb.append(" ORDER BY startDate DESC");
        return sb.toString();
    }

    private List<Object> buildListParams(UUID profileId, EventType eventType,
                                         LocalDate fromDate, LocalDate toDate) {
        List<Object> params = new ArrayList<>();
        params.add(profileId);
        if (eventType != null) params.add(eventType.name());
        if (fromDate != null) params.add(fromDate);
        if (toDate != null) params.add(toDate);
        return params;
    }
}
