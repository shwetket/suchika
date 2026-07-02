package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.output.DoctorVisitRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
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
    public List<DoctorVisit> findByProfileId(UUID profileId, LocalDate from, LocalDate to) {
        StringBuilder query = new StringBuilder("profileId = ?1");
        List<Object> params = new java.util.ArrayList<>();
        params.add(profileId);

        int paramIdx = 2;
        if (from != null) {
            query.append(" and fromDate >= ?").append(paramIdx++);
            params.add(from);
        }
        if (to != null) {
            query.append(" and fromDate <= ?").append(paramIdx);
            params.add(to);
        }
        query.append(" order by fromDate desc");

        return dao.find(query.toString(), params.toArray())
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
