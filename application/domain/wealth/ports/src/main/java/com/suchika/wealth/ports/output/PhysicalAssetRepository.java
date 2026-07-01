package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhysicalAssetRepository {

    PhysicalAsset save(PhysicalAsset asset);

    Optional<PhysicalAsset> findById(UUID id);

    List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive);

    boolean existsById(UUID id);

    boolean existsByRegistrationNumber(String registrationNumber);
}
