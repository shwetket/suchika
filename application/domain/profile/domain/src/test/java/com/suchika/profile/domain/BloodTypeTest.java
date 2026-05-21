package com.suchika.profile.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloodTypeTest {

    @Test
    void getLabel_returnsSymbolicLabel() {
        assertEquals("A+", BloodType.A_POSITIVE.getLabel());
        assertEquals("A-", BloodType.A_NEGATIVE.getLabel());
        assertEquals("B+", BloodType.B_POSITIVE.getLabel());
        assertEquals("B-", BloodType.B_NEGATIVE.getLabel());
        assertEquals("AB+", BloodType.AB_POSITIVE.getLabel());
        assertEquals("AB-", BloodType.AB_NEGATIVE.getLabel());
        assertEquals("O+", BloodType.O_POSITIVE.getLabel());
        assertEquals("O-", BloodType.O_NEGATIVE.getLabel());
    }

    @Test
    void fromLabel_validLabel_returnsMatchingEnum() {
        assertEquals(BloodType.A_POSITIVE, BloodType.fromLabel("A+"));
        assertEquals(BloodType.AB_NEGATIVE, BloodType.fromLabel("AB-"));
        assertEquals(BloodType.O_POSITIVE, BloodType.fromLabel("O+"));
    }

    @Test
    void fromLabel_null_returnsNull() {
        assertNull(BloodType.fromLabel(null));
    }

    @Test
    void fromLabel_unknownLabel_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> BloodType.fromLabel("X+"));
        assertThrows(IllegalArgumentException.class, () -> BloodType.fromLabel(""));
        assertThrows(IllegalArgumentException.class, () -> BloodType.fromLabel("a+"));
    }

    @Test
    void roundTrip_fromLabelGetLabel_isIdentity() {
        for (BloodType bt : BloodType.values()) {
            assertEquals(bt, BloodType.fromLabel(bt.getLabel()),
                "fromLabel(getLabel()) should return the same enum for " + bt.name());
        }
    }
}
