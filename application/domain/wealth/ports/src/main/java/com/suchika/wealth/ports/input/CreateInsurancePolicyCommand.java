package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;

import java.math.BigDecimal;

/**
 * admin_id is passed separately to {@link InsurancePolicyUseCase#createInsurancePolicy},
 * mirroring the CreateAccountCommand/CreateGoalPlanCommand precedent (tenant id kept
 * out of the command, Sonar S107).
 */
public record CreateInsurancePolicyCommand(
        String policyName,
        String provider,
        PolicyType policyType,
        BigDecimal premiumAmount,
        PremiumFrequency premiumFrequency,
        BigDecimal coverageAmount
) {}
