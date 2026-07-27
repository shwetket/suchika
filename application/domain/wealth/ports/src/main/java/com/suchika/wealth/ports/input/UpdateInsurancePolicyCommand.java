package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.PremiumFrequency;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Partial-update payload for {@link InsurancePolicyUseCase#updateInsurancePolicy}.
 * Non-null fields replace the existing value; payout_structure, if non-null, merges
 * into the existing map (mirrors the Account/PhysicalAsset/GoalPlan metadata/detail
 * merge pattern). admin_id and policy_type are immutable after creation — not
 * present here.
 */
public record UpdateInsurancePolicyCommand(
        String policyName,
        String provider,
        BigDecimal premiumAmount,
        PremiumFrequency premiumFrequency,
        BigDecimal coverageAmount,
        Map<String, String> payoutStructure,
        Boolean isActive
) {}
