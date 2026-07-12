package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.InsurancePolicy;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InsurancePolicyResponse {

    @JsonProperty("id")
    public UUID id;

    @JsonProperty("admin_id")
    public UUID adminId;

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

    @JsonProperty("payout_structure")
    public Map<String, String> payoutStructure;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("updated_at")
    public Instant updatedAt;

    public static InsurancePolicyResponse from(InsurancePolicy policy) {
        InsurancePolicyResponse r = new InsurancePolicyResponse();
        r.id = policy.getId();
        r.adminId = policy.getAdminId();
        r.policyName = policy.getPolicyName();
        r.provider = policy.getProvider();
        r.policyType = policy.getPolicyType() != null ? policy.getPolicyType().name() : null;
        r.premiumAmount = policy.getPremiumAmount();
        r.premiumFrequency = policy.getPremiumFrequency() != null ? policy.getPremiumFrequency().name() : null;
        r.coverageAmount = policy.getCoverageAmount();
        r.payoutStructure = policy.getPayoutStructure();
        r.active = policy.isActive();
        r.createdAt = policy.getCreatedAt();
        r.updatedAt = policy.getUpdatedAt();
        return r;
    }
}
