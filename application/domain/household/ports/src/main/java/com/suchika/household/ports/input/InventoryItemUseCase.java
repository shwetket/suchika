package com.suchika.household.ports.input;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryItemUseCase {

    InventoryItem create(UUID profileId, String itemName, BigDecimal quantity,
                         ItemUnit unit, SourcePlatform sourcePlatform,
                         LocalDate purchaseDate, String category);

    List<InventoryItem> list(UUID profileId, SourcePlatform sourcePlatform, String category);

    InventoryItem get(UUID id);

    InventoryItem update(UUID id, String itemName, BigDecimal quantity, ItemUnit unit,
                         SourcePlatform sourcePlatform, LocalDate purchaseDate,
                         String category, Boolean isConsumed);

    void delete(UUID id);
}
