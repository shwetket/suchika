package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
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

    @Column(name = "make", nullable = false, length = 100)
    public String make;

    @Column(name = "model", nullable = false, length = 100)
    public String model;

    @Column(name = "registration_number", nullable = false, length = 50)
    public String registrationNumber;

    @Column(name = "registration_type", nullable = false, length = 50)
    public String registrationType;

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
                .registrationType(RegistrationType.valueOf(registrationType))
                .active(active)
                .createdAt(createdAt)
                .metadata(JsonbMetadataUtil.read(metadata))
                .build();
    }
}
