package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "insurance_policy", schema = "wealth")
public class InsurancePolicyEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "admin_id", nullable = false, columnDefinition = "uuid")
    public UUID adminId;

    @Column(name = "policy_name", nullable = false, length = 50)
    public String policyName;

    @Column(name = "provider", nullable = false, length = 50)
    public String provider;

    @Column(name = "policy_type", nullable = false, length = 50)
    public String policyType;

    @Column(name = "premium_amount", nullable = false, precision = 19, scale = 4)
    public BigDecimal premiumAmount;

    @Column(name = "premium_frequency", nullable = false, length = 20)
    public String premiumFrequency;

    @Column(name = "coverage_amount", precision = 19, scale = 4)
    public BigDecimal coverageAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payout_structure", columnDefinition = "jsonb", nullable = false)
    public String payoutStructure = "{}";

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public static InsurancePolicyEntity from(InsurancePolicy policy) {
        InsurancePolicyEntity e = new InsurancePolicyEntity();
        e.id = policy.getId();
        e.adminId = policy.getAdminId();
        e.policyName = policy.getPolicyName();
        e.provider = policy.getProvider();
        e.policyType = policy.getPolicyType() != null ? policy.getPolicyType().name() : null;
        e.premiumAmount = policy.getPremiumAmount();
        e.premiumFrequency = policy.getPremiumFrequency() != null ? policy.getPremiumFrequency().name() : null;
        e.coverageAmount = policy.getCoverageAmount();
        e.payoutStructure = JsonbMetadataUtil.write(policy.getPayoutStructure());
        e.active = policy.isActive();
        e.createdAt = policy.getCreatedAt();
        e.updatedAt = policy.getUpdatedAt();
        return e;
    }

    public InsurancePolicy toDomain() {
        return InsurancePolicy.builder()
                .id(id)
                .adminId(adminId)
                .policyName(policyName)
                .provider(provider)
                .policyType(PolicyType.valueOf(policyType))
                .premiumAmount(premiumAmount)
                .premiumFrequency(PremiumFrequency.valueOf(premiumFrequency))
                .coverageAmount(coverageAmount)
                .payoutStructure(JsonbMetadataUtil.read(payoutStructure))
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
