package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;
import com.suchika.wealth.ports.input.CreateInsurancePolicyCommand;
import com.suchika.wealth.ports.input.UpdateInsurancePolicyCommand;
import com.suchika.wealth.ports.output.InsurancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InsurancePolicyServiceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private InsurancePolicyService service;
    private FakeInsurancePolicyRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FakeInsurancePolicyRepository();
        service = new InsurancePolicyService(repo);
    }

    private static CreateInsurancePolicyCommand cmd(String name, BigDecimal premium, PremiumFrequency frequency) {
        return new CreateInsurancePolicyCommand(name, "Provider", PolicyType.TERM, premium, frequency, null);
    }

    @Test
    void createInsurancePolicy_happyPath_returnsPolicyWithId() {
        InsurancePolicy result = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        assertNotNull(result.getId());
        assertEquals(ADMIN_ID, result.getAdminId());
        assertEquals("Term Plan", result.getPolicyName());
        assertTrue(result.isActive());
    }

    @Test
    void createInsurancePolicy_invalidPolicy_domainValidationThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createInsurancePolicy(ADMIN_ID, cmd("Term Plan", new BigDecimal("-1"), PremiumFrequency.MONTHLY)));
    }

    @Test
    void getInsurancePolicy_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getInsurancePolicy(UUID.randomUUID(), ADMIN_ID));
    }

    @Test
    void getInsurancePolicy_wrongAdmin_throwsNotFound() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        assertThrows(NotFoundException.class, () -> service.getInsurancePolicy(policy.getId(), UUID.randomUUID()));
    }

    @Test
    void listInsurancePolicies_scopedByAdmin() {
        service.createInsurancePolicy(ADMIN_ID, cmd("Plan 1", new BigDecimal("1500"), PremiumFrequency.MONTHLY));
        service.createInsurancePolicy(UUID.randomUUID(), cmd("Other household's plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        List<InsurancePolicy> result = service.listInsurancePolicies(ADMIN_ID);

        assertEquals(1, result.size());
        assertEquals("Plan 1", result.get(0).getPolicyName());
    }

    @Test
    void updateInsurancePolicy_updatesProvidedFieldsOnly() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        InsurancePolicy updated = service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand(null, null, new BigDecimal("2000"), null, null, null, null));

        assertEquals("Term Plan", updated.getPolicyName());
        assertEquals(new BigDecimal("2000"), updated.getPremiumAmount());
    }

    @Test
    void updateInsurancePolicy_mergesPayoutStructure_doesNotReplaceWholesale() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        Map<String, String> first = new HashMap<>();
        first.put("payout_type", "LUMP_SUM");
        service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand(null, null, null, null, null, first, null));

        Map<String, String> second = new HashMap<>();
        second.put("maturity_year", "2045");
        InsurancePolicy updated = service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand(null, null, null, null, null, second, null));

        assertEquals("LUMP_SUM", updated.getPayoutStructure().get("payout_type"));
        assertEquals("2045", updated.getPayoutStructure().get("maturity_year"));
    }

    @Test
    void updateInsurancePolicy_blankPolicyName_throwsBadRequest() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        assertThrows(BadRequestException.class, () -> service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand("   ", null, null, null, null, null, null)));
    }

    @Test
    void updateInsurancePolicy_negativePremiumAmount_throwsBadRequest() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        assertThrows(BadRequestException.class, () -> service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand(null, null, new BigDecimal("-1"), null, null, null, null)));
    }

    @Test
    void updateInsurancePolicy_negativeCoverageAmount_throwsBadRequest() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        assertThrows(BadRequestException.class, () -> service.updateInsurancePolicy(policy.getId(), ADMIN_ID,
                new UpdateInsurancePolicyCommand(null, null, null, null, new BigDecimal("-1"), null, null)));
    }

    @Test
    void updateInsurancePolicy_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.updateInsurancePolicy(UUID.randomUUID(), ADMIN_ID,
                new UpdateInsurancePolicyCommand("Name", null, null, null, null, null, null)));
    }

    @Test
    void deactivateInsurancePolicy_setsActiveFalse() {
        InsurancePolicy policy = service.createInsurancePolicy(ADMIN_ID,
                cmd("Term Plan", new BigDecimal("1500"), PremiumFrequency.MONTHLY));

        service.deactivateInsurancePolicy(policy.getId(), ADMIN_ID);

        assertFalse(service.getInsurancePolicy(policy.getId(), ADMIN_ID).isActive());
    }

    @Test
    void deactivateInsurancePolicy_notFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.deactivateInsurancePolicy(UUID.randomUUID(), ADMIN_ID));
    }

    /** Minimal in-memory fake — mirrors GoalPlanServiceTest's FakeGoalPlanRepository pattern. */
    static class FakeInsurancePolicyRepository implements InsurancePolicyRepository {
        private final Map<UUID, InsurancePolicy> policies = new HashMap<>();

        @Override
        public InsurancePolicy save(InsurancePolicy insurancePolicy) {
            UUID id = insurancePolicy.getId() != null ? insurancePolicy.getId() : UUID.randomUUID();
            InsurancePolicy stored = InsurancePolicy.builder()
                    .id(id)
                    .adminId(insurancePolicy.getAdminId())
                    .policyName(insurancePolicy.getPolicyName())
                    .provider(insurancePolicy.getProvider())
                    .policyType(insurancePolicy.getPolicyType())
                    .premiumAmount(insurancePolicy.getPremiumAmount())
                    .premiumFrequency(insurancePolicy.getPremiumFrequency())
                    .coverageAmount(insurancePolicy.getCoverageAmount())
                    .payoutStructure(new HashMap<>(insurancePolicy.getPayoutStructure()))
                    .active(insurancePolicy.isActive())
                    .createdAt(java.time.Instant.now())
                    .updatedAt(java.time.Instant.now())
                    .build();
            policies.put(id, stored);
            return stored;
        }

        @Override
        public Optional<InsurancePolicy> findById(UUID id, UUID adminId) {
            InsurancePolicy policy = policies.get(id);
            if (policy == null || !policy.getAdminId().equals(adminId)) {
                return Optional.empty();
            }
            return Optional.of(policy);
        }

        @Override
        public List<InsurancePolicy> findAll(UUID adminId) {
            return policies.values().stream().filter(p -> p.getAdminId().equals(adminId)).toList();
        }
    }
}
