package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PhysicalAssetUseCase {

    PhysicalAsset createAsset(UUID profileId, CreatePhysicalAssetCommand command);

    /**
     * Profile-scoped lookup — v0.5.1 remediation (Tier B). Throws NotFoundException
     * both when the id doesn't exist AND when it belongs to a different profile.
     */
    PhysicalAsset getAsset(UUID id, UUID profileId);

    List<PhysicalAsset> listAssets(UUID profileId, AssetType assetType, Boolean isActive);

    /**
     * Paginated variant of {@link #listAssets} — extends the v0.6 transaction list
     * pagination pattern to physical assets (Q54). {@code page} is 0-indexed. Used
     * by the HTTP list endpoint; {@link #listAssets} stays as-is for any caller
     * that wants the full list.
     */
    PagedPhysicalAssets listAssetsPaginated(UUID profileId, AssetType assetType, Boolean isActive, int page, int size);

    /**
     * Partial update. Non-null scalar fields replace the existing value. metadata, if
     * non-null, is merged into the existing metadata map (compliance deadlines etc.) —
     * mirrors the Account.metadata merge pattern, never a wholesale replace.
     */
    PhysicalAsset updateAsset(UUID id, UUID profileId, String assetName, String make, String model,
                               Map<String, String> metadata, Boolean isActive);

    void deactivateAsset(UUID id, UUID profileId);
}
