package com.suchika.household.ports.output;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository {

    Goal save(Goal goal);

    Optional<Goal> findById(UUID id);

    List<Goal> findByProfileId(UUID profileId, GoalStatus status);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
