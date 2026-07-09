package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.LocalDate;

@RegisterForReflection
public class CreatePhysicalAssetRequest {

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
}
