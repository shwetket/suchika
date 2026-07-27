package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsurancePolicyTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Test
    void create_happyPath_returnsActivePolicy() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "HDFC Term Plan", "HDFC Life",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, new BigDecimal("5000000"));

        assertEquals(ADMIN_ID, policy.getAdminId());
        assertEquals("HDFC Term Plan", policy.getPolicyName());
        assertEquals("HDFC Life", policy.getProvider());
        assertEquals(PolicyType.TERM, policy.getPolicyType());
        assertEquals(new BigDecimal("1500"), policy.getPremiumAmount());
        assertEquals(PremiumFrequency.MONTHLY, policy.getPremiumFrequency());
        assertEquals(new BigDecimal("5000000"), policy.getCoverageAmount());
        assertTrue(policy.isActive());
        assertNotNull(policy.getPayoutStructure());
        assertTrue(policy.getPayoutStructure().isEmpty());
    }

    @Test
    void create_nullCoverageAmount_allowed() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Group Health", "Star Health",
                PolicyType.HEALTH, new BigDecimal("2000"), PremiumFrequency.ANNUAL, null);

        assertEquals(null, policy.getCoverageAmount());
    }

    @Test
    void create_nullAdminId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(null, "Name", "Provider",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_blankPolicyName_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "  ", "Provider",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_policyNameExceeds50Chars_throwsIllegalArgument() {
        String tooLong = "A".repeat(51);
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, tooLong, "Provider",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_blankProvider_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_nullPolicyType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                null, BigDecimal.TEN, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_nullPremiumAmount_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, null, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_negativePremiumAmount_throwsIllegalArgument() {
        BigDecimal negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, negative, PremiumFrequency.MONTHLY, null));
    }

    @Test
    void create_zeroPremiumAmount_allowed() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, BigDecimal.ZERO, PremiumFrequency.MONTHLY, null);

        assertEquals(BigDecimal.ZERO, policy.getPremiumAmount());
    }

    @Test
    void create_nullPremiumFrequency_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, BigDecimal.TEN, null, null));
    }

    @Test
    void create_negativeCoverageAmount_throwsIllegalArgument() {
        BigDecimal negative = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, negative));
    }

    @Test
    void monthlyPremium_monthlyFrequency_passesThroughUnchanged() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, new BigDecimal("1500"), PremiumFrequency.MONTHLY, null);

        assertEquals(0, new BigDecimal("1500").compareTo(policy.monthlyPremium()));
    }

    @Test
    void monthlyPremium_annualFrequency_dividesByTwelve() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, new BigDecimal("12000"), PremiumFrequency.ANNUAL, null);

        assertEquals(0, new BigDecimal("1000.0000").compareTo(policy.monthlyPremium()));
    }

    @Test
    void monthlyPremium_annualFrequency_nonExactDivision_roundsHalfUp() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, new BigDecimal("1000"), PremiumFrequency.ANNUAL, null);

        // 1000 / 12 = 83.3333... rounded to 4 decimal places, half-up
        assertEquals(0, new BigDecimal("83.3333").compareTo(policy.monthlyPremium()));
    }

    @Test
    void noArgConstructor_initializesEmptyPayoutStructure() {
        InsurancePolicy policy = new InsurancePolicy();

        assertNotNull(policy.getPayoutStructure());
        assertTrue(policy.getPayoutStructure().isEmpty());
    }

    @Test
    void builder_withIdCreatedAtUpdatedAt_roundTripsThroughGetters() {
        UUID id = UUID.randomUUID();
        java.time.Instant createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z");
        java.time.Instant updatedAt = java.time.Instant.parse("2026-02-01T00:00:00Z");

        InsurancePolicy policy = InsurancePolicy.builder()
                .id(id)
                .adminId(ADMIN_ID)
                .policyName("Reconstructed Policy")
                .provider("Provider")
                .policyType(PolicyType.HEALTH)
                .premiumAmount(new BigDecimal("999"))
                .premiumFrequency(PremiumFrequency.MONTHLY)
                .coverageAmount(new BigDecimal("1000000"))
                .active(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertEquals(id, policy.getId());
        assertEquals(createdAt, policy.getCreatedAt());
        assertEquals(updatedAt, policy.getUpdatedAt());
        assertEquals(false, policy.isActive());
    }

    @Test
    void setters_mutateFieldsInPlace() {
        InsurancePolicy policy = InsurancePolicy.create(ADMIN_ID, "Original", "Original Provider",
                PolicyType.TERM, new BigDecimal("100"), PremiumFrequency.MONTHLY, null);

        java.time.Instant updatedAt = java.time.Instant.parse("2026-03-01T00:00:00Z");
        java.util.Map<String, String> payout = java.util.Map.of("lump_sum", "true");

        policy.setPolicyName("Renamed");
        policy.setProvider("New Provider");
        policy.setPremiumAmount(new BigDecimal("200"));
        policy.setPremiumFrequency(PremiumFrequency.ANNUAL);
        policy.setCoverageAmount(new BigDecimal("500000"));
        policy.setPayoutStructure(payout);
        policy.setActive(false);
        policy.setUpdatedAt(updatedAt);

        assertEquals("Renamed", policy.getPolicyName());
        assertEquals("New Provider", policy.getProvider());
        assertEquals(0, new BigDecimal("200").compareTo(policy.getPremiumAmount()));
        assertEquals(PremiumFrequency.ANNUAL, policy.getPremiumFrequency());
        assertEquals(0, new BigDecimal("500000").compareTo(policy.getCoverageAmount()));
        assertEquals(payout, policy.getPayoutStructure());
        assertEquals(false, policy.isActive());
        assertEquals(updatedAt, policy.getUpdatedAt());
    }
}
