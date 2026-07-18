package com.suchika.wealth.adapters.persistence;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.sharedadapter.errorlog.AbstractErrorLogPanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Binds this domain's own {@link ErrorLogEntity}/{@link ErrorLogDao} to the
 * shared save/findSince query logic in {@link AbstractErrorLogPanacheRepository}
 * (2026-07-13 ADR-023 revision).
 */
@ApplicationScoped
public class ErrorLogPanacheRepository extends AbstractErrorLogPanacheRepository<ErrorLogEntity> {

    private final ErrorLogDao dao;

    public ErrorLogPanacheRepository(ErrorLogDao dao) {
        this.dao = dao;
    }

    @Override
    protected PanacheRepositoryBase<ErrorLogEntity, UUID> dao() {
        return dao;
    }

    @Override
    protected ErrorLogEntity newEntity(String errorCode, int httpStatus, String message, String details) {
        return ErrorLogEntity.from(errorCode, httpStatus, message, details);
    }

    @Override
    protected ErrorLog toDomain(ErrorLogEntity entity) {
        return entity.toDomain();
    }
}
