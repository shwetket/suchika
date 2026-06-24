package com.suchika.household.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryItemTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, 6, 24);

    @Test
    void create_happyPath_returnsItemWithAllFields() {
        InventoryItem item = InventoryItem.create(
                PROFILE_ID, "Amul Milk", new BigDecimal("2.5"),
                ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, "Dairy");

        assertNotNull(item);
        assertEquals(PROFILE_ID, item.getProfileId());
        assertEquals("Amul Milk", item.getItemName());
        assertEquals(0, new BigDecimal("2.5").compareTo(item.getQuantity()));
        assertEquals(ItemUnit.L, item.getUnit());
        assertEquals(SourcePlatform.INSTAMART, item.getSourcePlatform());
        assertEquals(PURCHASE_DATE, item.getPurchaseDate());
        assertEquals("Dairy", item.getCategory());
    }

    @Test
    void create_nullCategory_succeeds() {
        InventoryItem item = InventoryItem.create(
                PROFILE_ID, "Rice", new BigDecimal("5.0"),
                ItemUnit.KG, SourcePlatform.BLINKIT, PURCHASE_DATE, null);

        assertNotNull(item);
        assertEquals("Rice", item.getItemName());
    }

    @Test
    void create_zeroQuantity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Milk", BigDecimal.ZERO,
                        ItemUnit.L, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_negativeQuantity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Milk", new BigDecimal("-1"),
                        ItemUnit.L, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_blankItemName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "  ", new BigDecimal("1"),
                        ItemUnit.KG, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullItemName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, null, new BigDecimal("1"),
                        ItemUnit.KG, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullUnit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", new BigDecimal("1"),
                        null, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullSourcePlatform_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", new BigDecimal("1"),
                        ItemUnit.KG, null, PURCHASE_DATE, null));
    }

    @Test
    void create_nullPurchaseDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", new BigDecimal("1"),
                        ItemUnit.KG, SourcePlatform.MANUAL, null, null));
    }
}
