package com.suchika.household.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryItemTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, Month.JUNE, 24);

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
    void create_defaultsIsConsumedToFalse() {
        InventoryItem item = InventoryItem.create(
                PROFILE_ID, "Rice", new BigDecimal("5.0"),
                ItemUnit.KG, SourcePlatform.BLINKIT, PURCHASE_DATE, null);

        assertEquals(false, item.isConsumed());
    }

    @Test
    void builder_isConsumedTrue_reflectedOnGetter() {
        InventoryItem item = InventoryItem.builder()
                .profileId(PROFILE_ID)
                .itemName("Rice")
                .quantity(new BigDecimal("5.0"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.BLINKIT)
                .purchaseDate(PURCHASE_DATE)
                .consumed(true)
                .build();

        assertEquals(true, item.isConsumed());
    }

    @Test
    void create_zeroQuantity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Milk", BigDecimal.ZERO, ItemUnit.L, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_negativeQuantity_throwsIllegalArgumentException() {
        BigDecimal negativeOne = new BigDecimal("-1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Milk", negativeOne, ItemUnit.L, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_blankItemName_throwsIllegalArgumentException() {
        BigDecimal one = new BigDecimal("1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "  ", one, ItemUnit.KG, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullItemName_throwsIllegalArgumentException() {
        BigDecimal one = new BigDecimal("1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, null, one, ItemUnit.KG, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullUnit_throwsIllegalArgumentException() {
        BigDecimal one = new BigDecimal("1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", one, null, SourcePlatform.MANUAL, PURCHASE_DATE, null));
    }

    @Test
    void create_nullSourcePlatform_throwsIllegalArgumentException() {
        BigDecimal one = new BigDecimal("1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", one, ItemUnit.KG, null, PURCHASE_DATE, null));
    }

    @Test
    void create_nullPurchaseDate_throwsIllegalArgumentException() {
        BigDecimal one = new BigDecimal("1");
        assertThrows(IllegalArgumentException.class, () ->
                InventoryItem.create(PROFILE_ID, "Item", one, ItemUnit.KG, SourcePlatform.MANUAL, null, null));
    }
}
