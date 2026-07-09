package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.PhysicalAsset;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhysicalAssetResponse {

    @JsonProperty("asset_id")
    public UUID assetId;

    @JsonProperty("asset_name")
    public String assetName;

    @JsonProperty("asset_type")
    public String assetType;

    @JsonProperty("make")
    public String make;

    @JsonProperty("model")
    public String model;

    @JsonProperty("registration_number")
    public String registrationNumber;

    @JsonProperty("registration_type")
    public String registrationType;

    @JsonProperty("current_value")
    public BigDecimal currentValue;

    @JsonProperty("valuation_date")
    public LocalDate valuationDate;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("created_at")
    public Instant createdAt;

    /**
     * Compliance deadlines and optional vehicle details (puc_expiry, insurance_expiry,
     * road_tax_expiry, etc.) — see V2__physical_asset_valuation.sql for the valuation
     * columns and the original consolidated V1 migration for the metadata column itself.
     */
    @JsonProperty("metadata")
    public Map<String, String> metadata;

    public static PhysicalAssetResponse from(PhysicalAsset asset) {
        PhysicalAssetResponse r = new PhysicalAssetResponse();
        r.assetId = asset.getId();
        r.assetName = asset.getAssetName();
        r.assetType = asset.getAssetType() != null ? asset.getAssetType().name() : null;
        r.make = asset.getMake();
        r.model = asset.getModel();
        r.registrationNumber = asset.getRegistrationNumber();
        r.registrationType = asset.getRegistrationType() != null ? asset.getRegistrationType().name() : null;
        r.currentValue = asset.getCurrentValue();
        r.valuationDate = asset.getValuationDate();
        r.active = asset.isActive();
        r.createdAt = asset.getCreatedAt();
        r.metadata = asset.getMetadata();
        return r;
    }
}
