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

    /**
     * Paginated variant of {@link #findByProfileId} (Q54 pagination pass). {@code page}
     * is 0-indexed. Same filter predicate as the unpaginated method; use
     * {@link #countByProfileId} for the total matching count.
     */
    List<Goal> findByProfileId(UUID profileId, GoalStatus status, int page, int size);

    /**
     * Total count of goals matching the same filter predicate as
     * {@link #findByProfileId}, ignoring pagination — used to compute total pages.
     */
    long countByProfileId(UUID profileId, GoalStatus status);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
