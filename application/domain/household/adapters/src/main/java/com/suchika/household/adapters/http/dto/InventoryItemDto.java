package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.household.domain.InventoryItem;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryItemDto {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("item_name")
    public String itemName;

    @JsonProperty("quantity")
    public BigDecimal quantity;

    @JsonProperty("unit")
    public String unit;

    @JsonProperty("source_platform")
    public String sourcePlatform;

    @JsonProperty("purchase_date")
    public LocalDate purchaseDate;

    @JsonProperty("category")
    public String category;

    @JsonProperty("is_consumed")
    public boolean isConsumed;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static InventoryItemDto from(InventoryItem item) {
        InventoryItemDto dto = new InventoryItemDto();
        dto.id = item.getId();
        dto.profileId = item.getProfileId();
        dto.itemName = item.getItemName();
        dto.quantity = item.getQuantity();
        dto.unit = item.getUnit() != null ? item.getUnit().name() : null;
        dto.sourcePlatform = item.getSourcePlatform() != null ? item.getSourcePlatform().name() : null;
        dto.purchaseDate = item.getPurchaseDate();
        dto.category = item.getCategory();
        dto.isConsumed = item.isConsumed();
        dto.createdAt = item.getCreatedAt();
        return dto;
    }
}
