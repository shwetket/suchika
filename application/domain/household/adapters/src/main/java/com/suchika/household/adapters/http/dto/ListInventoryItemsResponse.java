package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public class ListInventoryItemsResponse {

    @JsonProperty("inventory_items")
    public List<InventoryItemDto> inventoryItems;

    @JsonProperty("total_size")
    public int totalSize;

    public ListInventoryItemsResponse(List<InventoryItemDto> inventoryItems) {
        this.inventoryItems = inventoryItems;
        this.totalSize = inventoryItems.size();
    }
}
