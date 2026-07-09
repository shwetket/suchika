package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhysicalAsset {

    private UUID id;
    private UUID profileId;
    private String assetName;
    private AssetType assetType;
    private String make;
    private String model;
    private String registrationNumber;
    private RegistrationType registrationType;
    private BigDecimal currentValue;
    private LocalDate valuationDate;
    private boolean active;
    private Instant createdAt;
    private Map<String, String> metadata;

    public PhysicalAsset() {}

    private PhysicalAsset(Builder builder) {
        this.id = builder.id;
        this.profileId = builder.profileId;
        this.assetName = builder.assetName;
        this.assetType = builder.assetType;
        this.make = builder.make;
        this.model = builder.model;
        this.registrationNumber = builder.registrationNumber;
        this.registrationType = builder.registrationType;
        this.currentValue = builder.currentValue;
        this.valuationDate = builder.valuationDate;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.metadata = builder.metadata;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID profileId;
        private String assetName;
        private AssetType assetType;
        private String make;
        private String model;
        private String registrationNumber;
        private RegistrationType registrationType;
        private BigDecimal currentValue;
        private LocalDate valuationDate;
        private boolean active = true;
        private Instant createdAt;
        private Map<String, String> metadata = new HashMap<>();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder profileId(UUID profileId) { this.profileId = profileId; return this; }
        public Builder assetName(String assetName) { this.assetName = assetName; return this; }
        public Builder assetType(AssetType assetType) { this.assetType = assetType; return this; }
        public Builder make(String make) { this.make = make; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder registrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; return this; }
        public Builder registrationType(RegistrationType registrationType) { this.registrationType = registrationType; return this; }
        public Builder currentValue(BigDecimal currentValue) { this.currentValue = currentValue; return this; }
        public Builder valuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public PhysicalAsset build() { return new PhysicalAsset(this); }
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getAssetName() { return assetName; }
    public AssetType getAssetType() { return assetType; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getRegistrationNumber() { return registrationNumber; }
    public RegistrationType getRegistrationType() { return registrationType; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public LocalDate getValuationDate() { return valuationDate; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, String> getMetadata() { return metadata; }

    public void setAssetName(String assetName) { this.assetName = assetName; }
    public void setMake(String make) { this.make = make; }
    public void setModel(String model) { this.model = model; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }
    public void setActive(boolean active) { this.active = active; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
