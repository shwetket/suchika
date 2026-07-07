package com.suchika.health.ports.input;

import com.suchika.health.domain.VitalReading;

import java.util.List;

/**
 * Result of a paginated vital reading list query — pre-v1.0 pagination pass (Q54).
 * {@code totalCount} is the count across all pages matching the same filter,
 * not just {@code readings.size()}.
 */
public record PagedVitalReadings(List<VitalReading> readings, long totalCount) {
}
