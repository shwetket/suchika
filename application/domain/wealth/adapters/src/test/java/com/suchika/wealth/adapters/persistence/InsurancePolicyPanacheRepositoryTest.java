package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;
import com.suchika.wealth.ports.output.InsurancePolicyRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InsurancePolicyPanacheRepository (ADR-022 Phase 2).
 * Requires a running local PostgreSQL (app_db). Each test runs in a transaction
 * that is rolled back on completion. Mirrors GoalPlanPanacheRepositoryTest's
 * saveAdmin()-style helper — insurance_policy is admin_id-scoped, same as goal_plan.
 */
@QuarkusTest
@TestProfile(InsurancePolicyPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class InsurancePolicyPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    @Inject
    InsurancePolicyRepository repository;

    @Inject
    EntityManager em;

    @Test
    void save_andFindById_roundTrip() {
        UUID adminId = saveAdmin();

        InsurancePolicy saved = repository.save(InsurancePolicy.create(adminId, "HDFC Term Plan", "HDFC Life",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, new BigDecimal("5000000")));

        assertNotNull(saved.getId());
        assertEquals(adminId, saved.getAdminId());
        assertEquals(PolicyType.TERM, saved.getPolicyType());
        assertEquals(PremiumFrequency.MONTHLY, saved.getPremiumFrequency());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<InsurancePolicy> found = repository.findById(saved.getId(), adminId);
        assertTrue(found.isPresent());
        assertEquals("HDFC Term Plan", found.get().getPolicyName());
        assertEquals(0, new BigDecimal("5000000").compareTo(found.get().getCoverageAmount()));
    }

    @Test
    void save_nullCoverageAmount_roundTripsAsNull() {
        UUID adminId = saveAdmin();

        InsurancePolicy saved = repository.save(InsurancePolicy.create(adminId, "Group Health", "Star Health",
                PolicyType.HEALTH, new BigDecimal("2000"), PremiumFrequency.ANNUAL, null));

        Optional<InsurancePolicy> found = repository.findById(saved.getId(), adminId);
        assertTrue(found.isPresent());
        assertNull(found.get().getCoverageAmount());
    }

    @Test
    void findById_wrongAdmin_returnsEmpty() {
        UUID ownerAdminId = saveAdmin();
        UUID otherAdminId = saveAdmin();
        InsurancePolicy saved = repository.save(InsurancePolicy.create(ownerAdminId, "Plan", "Provider",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, null));

        Optional<InsurancePolicy> found = repository.findById(saved.getId(), otherAdminId);

        assertTrue(found.isEmpty(), "A different admin_id must not be able to fetch another household's insurance policy");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void findAll_scopedByAdmin_excludesOtherHouseholds() {
        UUID ownerAdminId = saveAdmin();
        UUID otherAdminId = saveAdmin();
        repository.save(InsurancePolicy.create(ownerAdminId, "Plan 1", "Provider",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, null));
        repository.save(InsurancePolicy.create(ownerAdminId, "Plan 2", "Provider",
                PolicyType.HEALTH, new BigDecimal("2000"), PremiumFrequency.ANNUAL, null));
        repository.save(InsurancePolicy.create(otherAdminId, "Other household's plan", "Provider",
                PolicyType.TERM, new BigDecimal("1000"), PremiumFrequency.MONTHLY, null));

        List<InsurancePolicy> ownerPolicies = repository.findAll(ownerAdminId);

        assertEquals(2, ownerPolicies.size());
    }

    @Test
    void save_update_persistsChanges() {
        UUID adminId = saveAdmin();
        InsurancePolicy saved = repository.save(InsurancePolicy.create(adminId, "Plan", "Provider",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, null));

        saved.setPremiumAmount(new BigDecimal("2000"));
        saved.setActive(false);
        repository.save(saved);

        Optional<InsurancePolicy> found = repository.findById(saved.getId(), adminId);
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("2000").compareTo(found.get().getPremiumAmount()));
        assertFalse(found.get().isActive());
    }

    @Test
    void save_payoutStructure_roundTripsJsonb() {
        UUID adminId = saveAdmin();
        InsurancePolicy policy = InsurancePolicy.create(adminId, "Endowment Plan", "LIC",
                PolicyType.ENDOWMENT, new BigDecimal("5000"), PremiumFrequency.ANNUAL, new BigDecimal("1000000"));
        policy.getPayoutStructure().put("payout_type", "SUM_ASSURED_AT_MATURITY");
        policy.getPayoutStructure().put("maturity_year", "2045");

        InsurancePolicy saved = repository.save(policy);
        Optional<InsurancePolicy> found = repository.findById(saved.getId(), adminId);

        assertTrue(found.isPresent());
        assertEquals("SUM_ASSURED_AT_MATURITY", found.get().getPayoutStructure().get("payout_type"));
        assertEquals("2045", found.get().getPayoutStructure().get("maturity_year"));
    }

    // ---- Helpers ----

    private UUID saveAdmin() {
        UUID adminId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.admin (id, display_name) VALUES (?1, ?2)")
                .setParameter(1, adminId)
                .setParameter(2, "Test Admin")
                .executeUpdate();
        return adminId;
    }
}
