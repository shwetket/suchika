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

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
