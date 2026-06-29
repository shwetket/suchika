package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.UploadErrorLog;
import com.suchika.wealth.ports.output.UploadErrorLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UploadErrorLogPanacheRepository implements UploadErrorLogRepository {

    private final UploadErrorLogDao dao;

    public UploadErrorLogPanacheRepository(UploadErrorLogDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional
    public void save(UUID uploadId, String errorType, List<String> missingColumns, String errorDetail) {
        UploadErrorLogEntity entity = UploadErrorLogEntity.from(uploadId, errorType, missingColumns, errorDetail);
        dao.persist(entity);
    }

    @Override
    public List<UploadErrorLog> findByUploadId(UUID uploadId) {
        return dao.find("uploadId = ?1", uploadId)
                .stream()
                .map(UploadErrorLogEntity::toDomain)
                .toList();
    }
}
