package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "physical_asset", schema = "wealth")
public class PhysicalAssetEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "profile_id", columnDefinition = "uuid")
    public UUID profileId;

    @Column(name = "asset_name", nullable = false, length = 100)
    public String assetName;

    @Column(name = "asset_type", nullable = false, length = 50)
    public String assetType;

    // make/model/registration_number/registration_type are genuinely optional — the DB
    // itself never had NOT NULL on these (see V1__init_wealth_consolidated.sql); they only
    // make sense for AssetType.VEHICLE. Non-vehicle assets (REAL_ESTATE, GOLD_JEWELRY,
    // GOLD_BOND) leave all four null.
    @Column(name = "make", length = 100)
    public String make;

    @Column(name = "model", length = 100)
    public String model;

    @Column(name = "registration_number", length = 50)
    public String registrationNumber;

    @Column(name = "registration_type", length = 50)
    public String registrationType;

    @Column(name = "current_value", precision = 19, scale = 4)
    public BigDecimal currentValue;

    @Column(name = "valuation_date")
    public LocalDate valuationDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    public String metadata = "{}";

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static PhysicalAssetEntity from(PhysicalAsset asset) {
        PhysicalAssetEntity e = new PhysicalAssetEntity();
        e.id = asset.getId();
        e.profileId = asset.getProfileId();
        e.assetName = asset.getAssetName();
        e.assetType = asset.getAssetType() != null ? asset.getAssetType().name() : null;
        e.make = asset.getMake();
        e.model = asset.getModel();
        e.registrationNumber = asset.getRegistrationNumber();
        e.registrationType = asset.getRegistrationType() != null ? asset.getRegistrationType().name() : null;
        e.currentValue = asset.getCurrentValue();
        e.valuationDate = asset.getValuationDate();
        e.active = asset.isActive();
        e.createdAt = asset.getCreatedAt();
        e.metadata = JsonbMetadataUtil.write(asset.getMetadata());
        return e;
    }

    public PhysicalAsset toDomain() {
        return PhysicalAsset.builder()
                .id(id)
                .profileId(profileId)
                .assetName(assetName)
                .assetType(AssetType.valueOf(assetType))
                .make(make)
                .model(model)
                .registrationNumber(registrationNumber)
                .registrationType(registrationType != null ? RegistrationType.valueOf(registrationType) : null)
                .currentValue(currentValue)
                .valuationDate(valuationDate)
                .active(active)
                .createdAt(createdAt)
                .metadata(JsonbMetadataUtil.read(metadata))
                .build();
    }
}
