package com.suchika.household.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class InventoryItem {

    private final UUID id;
    private final UUID profileId;
    private final String itemName;
    private final BigDecimal quantity;
    private final ItemUnit unit;
    private final SourcePlatform sourcePlatform;
    private final LocalDate purchaseDate;
    private final String category;
    private final Instant createdAt;

    private InventoryItem(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.itemName = builder.itemName;
        this.quantity = builder.quantity;
        this.unit = builder.unit;
        this.sourcePlatform = builder.sourcePlatform;
        this.purchaseDate = builder.purchaseDate;
        this.category = builder.category;
        this.createdAt = builder.createdAt;
    }

    public static InventoryItem create(UUID profileId, String itemName, BigDecimal quantity,
                                       ItemUnit unit, SourcePlatform sourcePlatform,
                                       LocalDate purchaseDate, String category) {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("item_name must not be blank");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (unit == null) {
            throw new IllegalArgumentException("unit must not be null");
        }
        if (sourcePlatform == null) {
            throw new IllegalArgumentException("source_platform must not be null");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("purchase_date must not be null");
        }
        return new Builder()
                .profileId(profileId)
                .itemName(itemName)
                .quantity(quantity)
                .unit(unit)
                .sourcePlatform(sourcePlatform)
                .purchaseDate(purchaseDate)
                .category(category)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private String itemName;
        private BigDecimal quantity;
        private ItemUnit unit;
        private SourcePlatform sourcePlatform;
        private LocalDate purchaseDate;
        private String category;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder unit(ItemUnit unit) { this.unit = unit; return this; }
        public Builder sourcePlatform(SourcePlatform sourcePlatform) { this.sourcePlatform = sourcePlatform; return this; }
        public Builder purchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public InventoryItem build() {
            return new InventoryItem(this);
        }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getItemName() { return itemName; }
    public BigDecimal getQuantity() { return quantity; }
    public ItemUnit getUnit() { return unit; }
    public SourcePlatform getSourcePlatform() { return sourcePlatform; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
