package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public class CreateInsurancePolicyRequest {

    @JsonProperty("policy_name")
    public String policyName;

    @JsonProperty("provider")
    public String provider;

    @JsonProperty("policy_type")
    public String policyType;

    @JsonProperty("premium_amount")
    public BigDecimal premiumAmount;

    @JsonProperty("premium_frequency")
    public String premiumFrequency;

    @JsonProperty("coverage_amount")
    public BigDecimal coverageAmount;
}
