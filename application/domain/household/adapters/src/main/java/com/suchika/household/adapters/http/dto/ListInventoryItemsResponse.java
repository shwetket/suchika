package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListInventoryItemsResponse {

    @JsonProperty("inventory_items")
    public List<InventoryItemDto> inventoryItems;

    @JsonProperty("total_size")
    public long totalSize;

    @JsonProperty("page")
    public Integer page;

    @JsonProperty("size")
    public Integer size;

    public ListInventoryItemsResponse(List<InventoryItemDto> inventoryItems) {
        this.inventoryItems = inventoryItems;
        this.totalSize = inventoryItems.size();
    }

    /**
     * Paginated variant (Q54 pagination pass) — totalSize is the grand total matching
     * the filter across all pages, not just inventoryItems.size().
     */
    public ListInventoryItemsResponse(List<InventoryItemDto> inventoryItems, long totalSize, int page, int size) {
        this.inventoryItems = inventoryItems;
        this.totalSize = totalSize;
        this.page = page;
        this.size = size;
    }
}
