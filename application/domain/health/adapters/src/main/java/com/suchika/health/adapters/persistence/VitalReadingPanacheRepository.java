package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.output.VitalReadingRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VitalReadingPanacheRepository implements VitalReadingRepository {

    private final VitalReadingDao dao;

    public VitalReadingPanacheRepository(VitalReadingDao dao) {
        this.dao = dao;
    }

    @Override
    public VitalReading save(VitalReading reading) {
        VitalReadingEntity entity = VitalReadingEntity.from(reading);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<VitalReading> findById(UUID id) {
        return dao.findByIdOptional(id).map(VitalReadingEntity::toDomain);
    }

    @Override
    public List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType) {
        if (vitalType != null) {
            return dao.find("profileId = ?1 and vitalType = ?2 order by readingDate desc",
                            profileId, vitalType.name())
                    .stream().map(VitalReadingEntity::toDomain).toList();
        }
        return dao.find("profileId = ?1 order by readingDate desc", profileId)
                .stream().map(VitalReadingEntity::toDomain).toList();
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
