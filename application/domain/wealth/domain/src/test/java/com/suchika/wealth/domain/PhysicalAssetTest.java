package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalAssetTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-01T10:00:00Z");
        LocalDate valuationDate = LocalDate.of(2026, Month.JUNE, 1);
        Map<String, String> metadata = Map.of("puc_expiry", "2027-01-01");

        PhysicalAsset asset = PhysicalAsset.builder()
                .id(id)
                .profileId(profileId)
                .assetName("Honda City")
                .assetType(AssetType.VEHICLE)
                .make("Honda")
                .model("City")
                .registrationNumber("MH12AB1234")
                .registrationType(RegistrationType.PRIVATE)
                .currentValue(new BigDecimal("850000.00"))
                .valuationDate(valuationDate)
                .active(true)
                .createdAt(createdAt)
                .metadata(metadata)
                .build();

        assertEquals(id, asset.getId());
        assertEquals(profileId, asset.getProfileId());
        assertEquals("Honda City", asset.getAssetName());
        assertEquals(AssetType.VEHICLE, asset.getAssetType());
        assertEquals("Honda", asset.getMake());
        assertEquals("City", asset.getModel());
        assertEquals("MH12AB1234", asset.getRegistrationNumber());
        assertEquals(RegistrationType.PRIVATE, asset.getRegistrationType());
        assertEquals(0, new BigDecimal("850000.00").compareTo(asset.getCurrentValue()));
        assertEquals(valuationDate, asset.getValuationDate());
        assertTrue(asset.isActive());
        assertEquals(createdAt, asset.getCreatedAt());
        assertEquals(metadata, asset.getMetadata());
    }

    @Test
    void builder_realEstateShape_vehicleOnlyFieldsAreNull() {
        PhysicalAsset asset = PhysicalAsset.builder()
                .assetName("Flat 402, Skyline Apartments")
                .assetType(AssetType.REAL_ESTATE)
                .currentValue(new BigDecimal("9500000.00"))
                .valuationDate(LocalDate.of(2026, Month.JANUARY, 1))
                .build();

        assertNull(asset.getMake());
        assertNull(asset.getModel());
        assertNull(asset.getRegistrationNumber());
        assertNull(asset.getRegistrationType());
        assertEquals(AssetType.REAL_ESTATE, asset.getAssetType());
        assertTrue(asset.isActive());
        assertNotNull(asset.getMetadata());
        assertTrue(asset.getMetadata().isEmpty());
    }

    @Test
    void noArgConstructor_producesEmptyPhysicalAsset() {
        PhysicalAsset asset = new PhysicalAsset();

        assertNull(asset.getId());
        assertNull(asset.getAssetName());
        assertFalse(asset.isActive());
    }

    @Test
    void setters_mutateCorrespondingFields() {
        PhysicalAsset asset = PhysicalAsset.builder()
                .assetName("Gold Coins")
                .assetType(AssetType.GOLD_JEWELRY)
                .build();

        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put("insurance_expiry", "2027-05-01");

        asset.setAssetName("Gold Bars");
        asset.setMake("N/A");
        asset.setModel("N/A");
        asset.setCurrentValue(new BigDecimal("300000.00"));
        asset.setValuationDate(LocalDate.of(2026, Month.JULY, 1));
        asset.setActive(false);
        asset.setMetadata(newMetadata);

        assertEquals("Gold Bars", asset.getAssetName());
        assertEquals("N/A", asset.getMake());
        assertEquals("N/A", asset.getModel());
        assertEquals(0, new BigDecimal("300000.00").compareTo(asset.getCurrentValue()));
        assertEquals(LocalDate.of(2026, Month.JULY, 1), asset.getValuationDate());
        assertFalse(asset.isActive());
        assertEquals(newMetadata, asset.getMetadata());
    }
}
