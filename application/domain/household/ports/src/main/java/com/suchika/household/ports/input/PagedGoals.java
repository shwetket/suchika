package com.suchika.household.ports.input;

import com.suchika.household.domain.Goal;

import java.util.List;

/**
 * Result of a paginated goal list query (Q54 pagination pass).
 * {@code totalCount} is the count across all pages matching the same filter,
 * not just {@code goals.size()}.
 */
public record PagedGoals(List<Goal> goals, long totalCount) {
}
