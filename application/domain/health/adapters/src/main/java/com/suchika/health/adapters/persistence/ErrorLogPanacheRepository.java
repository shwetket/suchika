package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.ErrorLog;
import com.suchika.health.ports.output.ErrorLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ErrorLogPanacheRepository implements ErrorLogRepository {

    private final ErrorLogDao dao;

    public ErrorLogPanacheRepository(ErrorLogDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional
    public void save(String errorCode, int httpStatus, String message, String details) {
        ErrorLogEntity entity = ErrorLogEntity.from(errorCode, httpStatus, message, details);
        dao.persist(entity);
    }

    @Override
    public List<ErrorLog> findSince(Instant since, int limit) {
        var query = since != null
                ? dao.find("createdAt >= ?1 order by createdAt desc", since)
                : dao.find("order by createdAt desc");
        return query.page(0, limit)
                .stream()
                .map(ErrorLogEntity::toDomain)
                .toList();
    }
}
