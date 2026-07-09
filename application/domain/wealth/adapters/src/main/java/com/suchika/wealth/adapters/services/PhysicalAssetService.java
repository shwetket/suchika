package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.ports.input.CreatePhysicalAssetCommand;
import com.suchika.wealth.ports.input.PagedPhysicalAssets;
import com.suchika.wealth.ports.input.PhysicalAssetUseCase;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class PhysicalAssetService implements PhysicalAssetUseCase {

    private static final String ASSET_NOT_FOUND = "Physical asset not found: ";

    private final PhysicalAssetRepository repository;

    public PhysicalAssetService(PhysicalAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PhysicalAsset createAsset(UUID profileId, CreatePhysicalAssetCommand command) {
        if (command.assetName() == null || command.assetName().isBlank()) {
            throw new BadRequestException("asset_name is required");
        }
        if (command.assetType() == null) {
            throw new BadRequestException("asset_type is required");
        }
        if (command.make() == null || command.make().isBlank()) {
            throw new BadRequestException("make is required");
        }
        if (command.model() == null || command.model().isBlank()) {
            throw new BadRequestException("model is required");
        }
        if (command.registrationNumber() == null || command.registrationNumber().isBlank()) {
            throw new BadRequestException("registration_number is required");
        }
        if (command.registrationType() == null) {
            throw new BadRequestException("registration_type is required");
        }
        if (repository.existsByRegistrationNumber(command.registrationNumber())) {
            throw new ConflictException("Asset already registered: " + command.registrationNumber());
        }

        PhysicalAsset asset = PhysicalAsset.builder()
                .profileId(profileId)
                .assetName(command.assetName())
                .assetType(command.assetType())
                .make(command.make())
                .model(command.model())
                .registrationNumber(command.registrationNumber())
                .registrationType(command.registrationType())
                .build();

        PhysicalAsset saved = repository.save(asset);
        AppLogger.info("Physical asset created: %s (%s)", saved.getId(), command.assetType());
        return saved;
    }

    @Override
    public PhysicalAsset getAsset(UUID id, UUID profileId) {
        return repository.findById(id, profileId)
                .orElseThrow(() -> new NotFoundException(ASSET_NOT_FOUND + id));
    }

    @Override
    public List<PhysicalAsset> listAssets(UUID profileId, AssetType assetType, Boolean isActive) {
        return repository.findAll(profileId, assetType, isActive);
    }

    @Override
    public PagedPhysicalAssets listAssetsPaginated(UUID profileId, AssetType assetType, Boolean isActive,
                                                    int page, int size) {
        List<PhysicalAsset> assets = repository.findAll(profileId, assetType, isActive, page, size);
        long totalCount = repository.countAll(profileId, assetType, isActive);
        return new PagedPhysicalAssets(assets, totalCount);
    }

    @Override
    @Transactional
    public PhysicalAsset updateAsset(UUID id, UUID profileId, String assetName, String make, String model,
                                       Map<String, String> metadata, Boolean isActive) {
        PhysicalAsset asset = repository.findById(id, profileId)
                .orElseThrow(() -> new NotFoundException(ASSET_NOT_FOUND + id));

        if (assetName != null) {
            if (assetName.isBlank()) throw new BadRequestException("asset_name must not be blank");
            asset.setAssetName(assetName);
        }
        if (make != null) asset.setMake(make);
        if (model != null) asset.setModel(model);
        if (metadata != null) {
            Map<String, String> merged = new HashMap<>(asset.getMetadata());
            merged.putAll(metadata);
            asset.setMetadata(merged);
        }
        if (isActive != null) asset.setActive(isActive);

        return repository.save(asset);
    }

    @Override
    @Transactional
    public void deactivateAsset(UUID id, UUID profileId) {
        PhysicalAsset asset = repository.findById(id, profileId)
                .orElseThrow(() -> new NotFoundException(ASSET_NOT_FOUND + id));

        asset.setActive(false);
        repository.save(asset);
        AppLogger.info("Physical asset deactivated: %s", id);
    }
}
