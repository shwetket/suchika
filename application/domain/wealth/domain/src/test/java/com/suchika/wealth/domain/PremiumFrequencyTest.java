package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PremiumFrequencyTest {

    @Test
    void values_matchDocumentedTwoValueContract() {
        PremiumFrequency[] values = PremiumFrequency.values();

        assertEquals(2, values.length);
        assertEquals(PremiumFrequency.MONTHLY, PremiumFrequency.valueOf("MONTHLY"));
        assertEquals(PremiumFrequency.ANNUAL, PremiumFrequency.valueOf("ANNUAL"));
    }
}
