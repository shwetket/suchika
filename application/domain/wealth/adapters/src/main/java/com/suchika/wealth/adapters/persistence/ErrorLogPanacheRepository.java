package com.suchika.wealth.adapters.persistence;

import com.suchika.sharedadapter.errorlog.AbstractErrorLogPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ErrorLogPanacheRepository extends AbstractErrorLogPanacheRepository<ErrorLogEntity> {
    protected ErrorLogPanacheRepository() { super(null, null); }
    @Inject
    public ErrorLogPanacheRepository(ErrorLogDao dao) {
        super(dao, ErrorLogEntity::new);
    }
}