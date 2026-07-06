package com.suchika.health.ports.input;

import com.suchika.health.domain.DoctorVisit;

import java.util.List;

/**
 * Result of a paginated doctor visit list query — pre-v1.0 pagination pass (Q54).
 * {@code totalCount} is the count across all pages matching the same filter,
 * not just {@code visits.size()}.
 */
public record PagedDoctorVisits(List<DoctorVisit> visits, long totalCount) {
}
