package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.UploadStatus;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StatementUploadPanacheRepository implements StatementUploadRepository {

    private final StatementUploadDao dao;

    public StatementUploadPanacheRepository(StatementUploadDao dao) {
        this.dao = dao;
    }

    @Override
    public StatementUpload save(StatementUpload upload) {
        StatementUploadEntity entity = StatementUploadEntity.from(upload);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<StatementUpload> findById(UUID id) {
        return dao.findByIdOptional(id).map(StatementUploadEntity::toDomain);
    }

    @Override
    @Transactional
    public StatementUpload updateStatus(UUID id, UploadStatus status) {
        StatementUploadEntity entity = dao.findById(id);
        entity.status = status.name();
        return entity.toDomain();
    }

    @Override
    public List<StatementUpload> findByAccountId(UUID accountId) {
        return dao.find("accountId = ?1 order by uploadDate desc", accountId)
                .stream().map(StatementUploadEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        dao.deleteById(id);
    }
}
