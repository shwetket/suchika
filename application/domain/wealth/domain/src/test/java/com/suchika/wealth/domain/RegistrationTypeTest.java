package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationTypeTest {

    @Test
    void values_matchDocumentedFourValueContract() {
        RegistrationType[] values = RegistrationType.values();

        assertEquals(4, values.length);
        assertEquals(RegistrationType.PRIVATE, RegistrationType.valueOf("PRIVATE"));
        assertEquals(RegistrationType.COMMERCIAL, RegistrationType.valueOf("COMMERCIAL"));
        assertEquals(RegistrationType.GOVERNMENT, RegistrationType.valueOf("GOVERNMENT"));
        assertEquals(RegistrationType.BH_SERIES, RegistrationType.valueOf("BH_SERIES"));
    }
}
