package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhysicalAssetRepository {

    PhysicalAsset save(PhysicalAsset asset);

    Optional<PhysicalAsset> findById(UUID id);

    /**
     * Profile-scoped lookup — v0.5.1 remediation (Tier B). Returns empty both when
     * the id doesn't exist at all AND when it exists but belongs to a different
     * profile, so callers can 404 without leaking cross-profile existence.
     */
    Optional<PhysicalAsset> findById(UUID id, UUID profileId);

    List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive);

    /**
     * Paginated variant of {@link #findAll} — v1.0 pagination extension (Q54).
     * {@code page} is 0-indexed. Same filter predicate as the unpaginated method;
     * use {@link #countAll} for the total matching count.
     */
    List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive, int page, int size);

    /**
     * Total count of physical assets matching the same filter predicate as
     * {@link #findAll}, ignoring pagination — used to compute total pages.
     */
    long countAll(UUID profileId, AssetType assetType, Boolean isActive);

    boolean existsById(UUID id);

    boolean existsByRegistrationNumber(String registrationNumber);
}
