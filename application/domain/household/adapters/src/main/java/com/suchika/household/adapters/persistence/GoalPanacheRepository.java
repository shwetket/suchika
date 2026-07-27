package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.output.GoalRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import com.suchika.shared.persistence.PanacheQueryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class GoalPanacheRepository implements GoalRepository {

    private final GoalDao dao;

    public GoalPanacheRepository(GoalDao dao) {
        this.dao = dao;
    }

    @Override
    public Goal save(Goal goal) {
        GoalEntity entity = GoalEntity.from(goal);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Goal> findById(UUID id) {
        return dao.findByIdOptional(id).map(GoalEntity::toDomain);
    }

    @Override
    public List<Goal> findByProfileId(UUID profileId, GoalStatus status) {
        PanacheQueryFilter filter = buildFilter(profileId, status);
        return dao.find(filter.query(), filter.params().toArray())
                .stream().map(GoalEntity::toDomain).toList();
    }

    @Override
    public List<Goal> findByProfileId(UUID profileId, GoalStatus status, int page, int size) {
        PanacheQueryFilter filter = buildFilter(profileId, status);
        return dao.find(filter.query(), filter.params().toArray())
                .page(Page.of(page, size))
                .list()
                .stream().map(GoalEntity::toDomain).toList();
    }

    @Override
    public long countByProfileId(UUID profileId, GoalStatus status) {
        PanacheQueryFilter filter = buildFilter(profileId, status);
        return dao.find(filter.query(), filter.params().toArray()).count();
    }

    /**
     * Shared predicate builder for {@code findByProfileId} (both variants) and
     * {@code countByProfileId} — keeps the filter logic in exactly one place
     * (Sonar CPD) now that there are three call sites needing the same predicate.
     */
    private PanacheQueryFilter buildFilter(UUID profileId, GoalStatus status) {
        List<Object> params = new ArrayList<>();
        params.add(profileId);
        String query;
        if (status != null) {
            query = "profileId = ?1 AND status = ?2 ORDER BY createdAt DESC";
            params.add(status.name());
        } else {
            query = "profileId = ?1 ORDER BY createdAt DESC";
        }
        return new PanacheQueryFilter(query, params);
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
