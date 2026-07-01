package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PhysicalAssetUseCase {

    PhysicalAsset createAsset(UUID profileId, CreatePhysicalAssetCommand command);

    PhysicalAsset getAsset(UUID id);

    List<PhysicalAsset> listAssets(UUID profileId, AssetType assetType, Boolean isActive);

    /**
     * Partial update. Non-null scalar fields replace the existing value. metadata, if
     * non-null, is merged into the existing metadata map (compliance deadlines etc.) —
     * mirrors the Account.metadata merge pattern, never a wholesale replace.
     */
    PhysicalAsset updateAsset(UUID id, String assetName, String make, String model,
                               Map<String, String> metadata, Boolean isActive);

    void deactivateAsset(UUID id);
}
