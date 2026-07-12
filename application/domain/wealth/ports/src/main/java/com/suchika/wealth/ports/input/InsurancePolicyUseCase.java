package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.InsurancePolicy;

import java.util.List;
import java.util.UUID;

/**
 * ADR-022 Phase 2 — CRUD for household-level insurance policies. Every method is
 * scoped by {@code adminId} (the household unit), not {@code profileId} — same
 * shape as {@link GoalPlanUseCase}.
 */
public interface InsurancePolicyUseCase {

    InsurancePolicy createInsurancePolicy(UUID adminId, CreateInsurancePolicyCommand command);

    InsurancePolicy getInsurancePolicy(UUID id, UUID adminId);

    List<InsurancePolicy> listInsurancePolicies(UUID adminId);

    InsurancePolicy updateInsurancePolicy(UUID id, UUID adminId, UpdateInsurancePolicyCommand command);

    void deactivateInsurancePolicy(UUID id, UUID adminId);
}
