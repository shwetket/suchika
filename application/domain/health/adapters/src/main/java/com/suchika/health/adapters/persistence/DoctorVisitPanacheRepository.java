package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.output.DoctorVisitRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DoctorVisitPanacheRepository implements DoctorVisitRepository {

    private final DoctorVisitDao dao;

    public DoctorVisitPanacheRepository(DoctorVisitDao dao) {
        this.dao = dao;
    }

    @Override
    public DoctorVisit save(DoctorVisit visit) {
        DoctorVisitEntity entity = DoctorVisitEntity.from(visit);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<DoctorVisit> findById(UUID id) {
        return dao.findByIdOptional(id).map(DoctorVisitEntity::toDomain);
    }

    @Override
    public List<DoctorVisit> findByProfileId(UUID profileId) {
        return dao.find("profileId = ?1 order by fromDate desc", profileId)
                .stream().map(DoctorVisitEntity::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        dao.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.count("id = ?1", id) > 0;
    }
}
