package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.Map;

@RegisterForReflection
public class UpdateInsurancePolicyRequest {

    @JsonProperty("policy_name")
    public String policyName;

    @JsonProperty("provider")
    public String provider;

    @JsonProperty("premium_amount")
    public BigDecimal premiumAmount;

    @JsonProperty("premium_frequency")
    public String premiumFrequency;

    @JsonProperty("coverage_amount")
    public BigDecimal coverageAmount;

    /** Merged into the existing payout_structure map — never a wholesale replace. */
    @JsonProperty("payout_structure")
    public Map<String, String> payoutStructure;

    @JsonProperty("is_active")
    public Boolean active;
}
