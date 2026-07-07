package com.suchika.household.ports.output;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.SourcePlatform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findById(UUID id);

    List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category);

    /**
     * Paginated variant of {@link #findByProfileId} (Q54 pagination pass). {@code page}
     * is 0-indexed. Same filter predicate as the unpaginated method; use
     * {@link #countByProfileId} for the total matching count.
     */
    List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category,
                                        int page, int size);

    /**
     * Total count of items matching the same filter predicate as
     * {@link #findByProfileId}, ignoring pagination — used to compute total pages.
     */
    long countByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
