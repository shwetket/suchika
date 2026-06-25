package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.output.GoalRepository;
import jakarta.enterprise.context.ApplicationScoped;

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
        if (status != null) {
            return dao.find("profileId = ?1 AND status = ?2 ORDER BY createdAt DESC",
                    profileId, status.name())
                    .stream().map(GoalEntity::toDomain).toList();
        }
        return dao.find("profileId = ?1 ORDER BY createdAt DESC", profileId)
                .stream().map(GoalEntity::toDomain).toList();
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
