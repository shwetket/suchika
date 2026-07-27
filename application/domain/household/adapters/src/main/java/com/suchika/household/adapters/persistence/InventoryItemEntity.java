package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_item", schema = "household")
public class InventoryItemEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "item_name", nullable = false, length = 200)
    public String itemName;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    public BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    public String unit;

    @Column(name = "source_platform", nullable = false, length = 50)
    public String sourcePlatform;

    @Column(name = "purchase_date", nullable = false)
    public LocalDate purchaseDate;

    @Column(name = "category", length = 50)
    public String category;

    @Column(name = "is_consumed", nullable = false)
    public boolean isConsumed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (metadata == null) metadata = "{}";
    }

    public static InventoryItemEntity from(InventoryItem item) {
        InventoryItemEntity e = new InventoryItemEntity();
        e.id = item.getId();
        e.profileId = item.getProfileId();
        e.itemName = item.getItemName();
        e.quantity = item.getQuantity();
        e.unit = item.getUnit() != null ? item.getUnit().name() : null;
        e.sourcePlatform = item.getSourcePlatform() != null ? item.getSourcePlatform().name() : null;
        e.purchaseDate = item.getPurchaseDate();
        e.category = item.getCategory();
        e.isConsumed = item.isConsumed();
        e.createdAt = item.getCreatedAt();
        return e;
    }

    public InventoryItem toDomain() {
        return InventoryItem.builder()
                .id(id)
                .profileId(profileId)
                .itemName(itemName)
                .quantity(quantity)
                .unit(unit != null ? ItemUnit.valueOf(unit) : null)
                .sourcePlatform(sourcePlatform != null ? SourcePlatform.valueOf(sourcePlatform) : null)
                .purchaseDate(purchaseDate)
                .category(category)
                .consumed(isConsumed)
                .createdAt(createdAt)
                .build();
    }
}
