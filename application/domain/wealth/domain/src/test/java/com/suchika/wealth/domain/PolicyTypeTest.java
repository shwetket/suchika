package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the 5 documented ADR-022 Phase 2 policy shapes — a fixed, closed set.
 */
class PolicyTypeTest {

    @Test
    void values_matchDocumentedFiveValueContract() {
        PolicyType[] values = PolicyType.values();

        assertEquals(5, values.length);
        assertEquals(PolicyType.TERM, PolicyType.valueOf("TERM"));
        assertEquals(PolicyType.GROUP_TERM, PolicyType.valueOf("GROUP_TERM"));
        assertEquals(PolicyType.INVESTMENT_LINKED, PolicyType.valueOf("INVESTMENT_LINKED"));
        assertEquals(PolicyType.ENDOWMENT, PolicyType.valueOf("ENDOWMENT"));
        assertEquals(PolicyType.HEALTH, PolicyType.valueOf("HEALTH"));
    }
}
