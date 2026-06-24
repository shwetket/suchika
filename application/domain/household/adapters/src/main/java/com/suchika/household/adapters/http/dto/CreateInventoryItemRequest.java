package com.suchika.household.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
public class CreateInventoryItemRequest {

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
}
