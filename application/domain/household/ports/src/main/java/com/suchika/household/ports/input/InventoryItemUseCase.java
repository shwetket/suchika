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

    /**
     * Paginated variant of {@link #list} (Q54 pagination pass). {@code page} is
     * 0-indexed. Used by the HTTP list endpoint; {@link #list} stays as-is for
     * any caller that wants the full list.
     */
    PagedInventoryItems listPaginated(UUID profileId, SourcePlatform sourcePlatform, String category,
                                       int page, int size);

    InventoryItem get(UUID id);

    InventoryItem update(UUID id, UpdateInventoryItemCommand command);

    void delete(UUID id);
}
