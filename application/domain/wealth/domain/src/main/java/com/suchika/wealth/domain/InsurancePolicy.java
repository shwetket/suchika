package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-022 Phase 2 — an insurance policy feeding two live computations: the
 * gateway's THIRTY_SEVENTY_TARGET premium term (monthly-normalized sum across
 * active policies) and INSURANCE_FREE's "WITH insurance" raw-list comparison
 * (Phase 3, gateway-side, not built here). Household-scoped by {@code admin_id}
 * (ADR-006 extension, same shape as {@code goal_plan}/{@code policy_settings}) —
 * not {@code profile_id}.
 */
public class InsurancePolicy {

    private static final int NAME_MAX_LENGTH = 50;

    private UUID id;
    private UUID adminId;
    private String policyName;
    private String provider;
    private PolicyType policyType;
    private BigDecimal premiumAmount;
    private PremiumFrequency premiumFrequency;
    private BigDecimal coverageAmount;
    private Map<String, String> payoutStructure;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public InsurancePolicy() {
        this.payoutStructure = new HashMap<>();
    }

    private InsurancePolicy(Builder builder) {
        this.id = builder.id;
        this.adminId = builder.adminId;
        this.policyName = builder.policyName;
        this.provider = builder.provider;
        this.policyType = builder.policyType;
        this.premiumAmount = builder.premiumAmount;
        this.premiumFrequency = builder.premiumFrequency;
        this.coverageAmount = builder.coverageAmount;
        this.payoutStructure = builder.payoutStructure;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * Validating factory (ADR-020: no CHECK constraints, validate in domain).
     * {@code premium_amount} must be non-negative — a policy with a negative
     * premium is nonsensical and would silently corrupt the THIRTY_SEVENTY_TARGET
     * premium sum. {@code coverage_amount}, when provided, is held to the same
     * non-negative rule.
     */
    public static InsurancePolicy create(UUID adminId, String policyName, String provider, PolicyType policyType,
                                          BigDecimal premiumAmount, PremiumFrequency premiumFrequency,
                                          BigDecimal coverageAmount) {
        if (adminId == null) {
            throw new IllegalArgumentException("admin_id is required");
        }
        validateName(policyName, "policy_name");
        validateName(provider, "provider");
        if (policyType == null) {
            throw new IllegalArgumentException("policy_type is required");
        }
        if (premiumAmount == null) {
            throw new IllegalArgumentException("premium_amount is required");
        }
        if (premiumAmount.signum() < 0) {
            throw new IllegalArgumentException("premium_amount must not be negative");
        }
        if (premiumFrequency == null) {
            throw new IllegalArgumentException("premium_frequency is required");
        }
        if (coverageAmount != null && coverageAmount.signum() < 0) {
            throw new IllegalArgumentException("coverage_amount must not be negative");
        }

        return new Builder()
                .adminId(adminId)
                .policyName(policyName)
                .provider(provider)
                .policyType(policyType)
                .premiumAmount(premiumAmount)
                .premiumFrequency(premiumFrequency)
                .coverageAmount(coverageAmount)
                .payoutStructure(new HashMap<>())
                .active(true)
                .build();
    }

    private static void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + NAME_MAX_LENGTH + " characters");
        }
    }

    /**
     * The premium normalized to a monthly figure — ANNUAL divides by 12, MONTHLY
     * passes through unchanged. Used by the gateway's THIRTY_SEVENTY_TARGET step
     * to sum premiums across policies of mixed frequency (ADR-022 Phase 2).
     */
    public BigDecimal monthlyPremium() {
        if (premiumFrequency == PremiumFrequency.ANNUAL) {
            return premiumAmount.divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
        }
        return premiumAmount;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID adminId;
        private String policyName;
        private String provider;
        private PolicyType policyType;
        private BigDecimal premiumAmount;
        private PremiumFrequency premiumFrequency;
        private BigDecimal coverageAmount;
        private Map<String, String> payoutStructure = new HashMap<>();
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder adminId(UUID adminId) { this.adminId = adminId; return this; }
        public Builder policyName(String policyName) { this.policyName = policyName; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder policyType(PolicyType policyType) { this.policyType = policyType; return this; }
        public Builder premiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; return this; }
        public Builder premiumFrequency(PremiumFrequency premiumFrequency) { this.premiumFrequency = premiumFrequency; return this; }
        public Builder coverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; return this; }
        public Builder payoutStructure(Map<String, String> payoutStructure) { this.payoutStructure = payoutStructure; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public InsurancePolicy build() { return new InsurancePolicy(this); }
    }

    public UUID getId() { return id; }
    public UUID getAdminId() { return adminId; }
    public String getPolicyName() { return policyName; }
    public String getProvider() { return provider; }
    public PolicyType getPolicyType() { return policyType; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public PremiumFrequency getPremiumFrequency() { return premiumFrequency; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public Map<String, String> getPayoutStructure() { return payoutStructure; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
    public void setPremiumFrequency(PremiumFrequency premiumFrequency) { this.premiumFrequency = premiumFrequency; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }
    public void setPayoutStructure(Map<String, String> payoutStructure) { this.payoutStructure = payoutStructure; }
    public void setActive(boolean active) { this.active = active; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
