package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RegisterForReflection
public class UpdatePhysicalAssetRequest {

    @JsonProperty("asset_name")
    public String assetName;

    @JsonProperty("make")
    public String make;

    @JsonProperty("model")
    public String model;

    /**
     * Merged into the existing metadata map — never a wholesale replace.
     * Use this to set/update compliance deadlines (puc_expiry, insurance_expiry, etc.).
     */
    @JsonProperty("metadata")
    public Map<String, String> metadata;

    @JsonProperty("is_active")
    public Boolean active;

    /**
     * Refreshes a periodically-updated valuation (e.g. a property or gold holding's
     * current market value). Only applied when provided.
     */
    @JsonProperty("current_value")
    public BigDecimal currentValue;

    @JsonProperty("valuation_date")
    public LocalDate valuationDate;
}
