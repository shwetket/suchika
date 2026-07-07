package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.PhysicalAsset;

import java.util.List;

/**
 * Result of a paginated physical asset list query — extends the v0.6 transaction
 * list pagination pattern to physical assets (roadmap item Q54). {@code totalCount}
 * is the count across all pages matching the same filter, not just {@code assets.size()}.
 */
public record PagedPhysicalAssets(List<PhysicalAsset> assets, long totalCount) {
}
