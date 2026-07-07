package com.suchika.household.ports.input;

import com.suchika.household.domain.InventoryItem;

import java.util.List;

/**
 * Result of a paginated inventory item list query (Q54 pagination pass).
 * {@code totalCount} is the count across all pages matching the same filter,
 * not just {@code items.size()}.
 */
public record PagedInventoryItems(List<InventoryItem> items, long totalCount) {
}
