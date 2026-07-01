package com.suchika.wealth.adapters.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "physical_asset", schema = "wealth")
public class PhysicalAssetEntity extends PanacheEntityBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {};

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
        e.metadata = writeMetadata(asset.getMetadata());
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
                .metadata(readMetadata(metadata))
                .build();
    }

    private static String writeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            AppLogger.warn("Failed to serialize physical asset metadata, defaulting to empty object: %s", e.getMessage());
            return "{}";
        }
    }

    private static Map<String, String> readMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(json, METADATA_TYPE);
        } catch (java.io.IOException e) {
            AppLogger.warn("Failed to deserialize physical asset metadata, defaulting to empty map: %s", e.getMessage());
            return new HashMap<>();
        }
    }
}
