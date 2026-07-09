package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the exact 4-value asset type contract added in the v1.0 net-worth-model
 * pass (VEHICLE was the only original value; REAL_ESTATE/GOLD_JEWELRY/GOLD_BOND
 * added 2026-07-09) — see documents/domain-state/wealth.md.
 */
class AssetTypeTest {

    @Test
    void values_matchDocumentedFourValueContract() {
        AssetType[] values = AssetType.values();

        assertEquals(4, values.length);
        assertEquals(AssetType.VEHICLE, AssetType.valueOf("VEHICLE"));
        assertEquals(AssetType.REAL_ESTATE, AssetType.valueOf("REAL_ESTATE"));
        assertEquals(AssetType.GOLD_JEWELRY, AssetType.valueOf("GOLD_JEWELRY"));
        assertEquals(AssetType.GOLD_BOND, AssetType.valueOf("GOLD_BOND"));
    }
}
