package com.suchika.health.adapters.persistence;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.output.VitalReadingRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import com.suchika.shared.persistence.PanacheQueryFilter;

import java.util.ArrayList;
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
        Filter filter = buildFilter(profileId, vitalType);
        return dao.find(filter.query(), filter.params().toArray())
                .stream().map(VitalReadingEntity::toDomain).toList();
    }

    @Override
    public List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType, int page, int size) {
        Filter filter = buildFilter(profileId, vitalType);
        return dao.find(filter.query(), filter.params().toArray())
                .page(Page.of(page, size))
                .list()
                .stream().map(VitalReadingEntity::toDomain).toList();
    }

    @Override
    public long countByProfileId(UUID profileId, VitalType vitalType) {
        Filter filter = buildFilter(profileId, vitalType);
        return dao.find(filter.query(), filter.params().toArray()).count();
    }

    /**
     * Shared predicate builder for {@code findByProfileId} (both variants) and
     * {@code countByProfileId} — keeps the filter logic in exactly one place
     * (Sonar CPD) now that there are three call sites needing the same predicate.
     */
    private Filter buildFilter(UUID profileId, VitalType vitalType) {
        StringBuilder query = new StringBuilder("profileId = ?1");
        List<Object> params = new ArrayList<>();
        params.add(profileId);

        if (vitalType != null) {
            query.append(" and vitalType = ?2");
            params.add(vitalType.name());
        }
        query.append(" order by readingDate desc");

        return new Filter(query.toString(), params);
    }

    private record Filter(String query, List<Object> params) {
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
