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
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, new BigDecimal("-1"), PremiumFrequency.MONTHLY, null));
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
        assertThrows(IllegalArgumentException.class, () -> InsurancePolicy.create(ADMIN_ID, "Name", "Provider",
                PolicyType.TERM, BigDecimal.TEN, PremiumFrequency.MONTHLY, new BigDecimal("-1")));
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
}
