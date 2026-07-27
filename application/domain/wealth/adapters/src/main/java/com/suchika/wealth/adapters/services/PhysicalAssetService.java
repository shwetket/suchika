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
import com.suchika.wealth.ports.input.UpdatePhysicalAssetCommand;
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
        // make/model/registration_number/registration_type are genuinely optional — they
        // only make sense for AssetType.VEHICLE. Non-vehicle assets (REAL_ESTATE,
        // GOLD_JEWELRY, GOLD_BOND) legitimately have none of these set.
        if (command.registrationNumber() != null && !command.registrationNumber().isBlank()
                && repository.existsByRegistrationNumber(command.registrationNumber())) {
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
                .currentValue(command.currentValue())
                .valuationDate(command.valuationDate())
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
    public PhysicalAsset updateAsset(UUID id, UUID profileId, UpdatePhysicalAssetCommand command) {
        PhysicalAsset asset = repository.findById(id, profileId)
                .orElseThrow(() -> new NotFoundException(ASSET_NOT_FOUND + id));

        String assetName = command.assetName();
        if (assetName != null) {
            if (assetName.isBlank()) throw new BadRequestException("asset_name must not be blank");
            asset.setAssetName(assetName);
        }
        if (command.make() != null) asset.setMake(command.make());
        if (command.model() != null) asset.setModel(command.model());
        if (command.metadata() != null) {
            Map<String, String> merged = new HashMap<>(asset.getMetadata());
            merged.putAll(command.metadata());
            asset.setMetadata(merged);
        }
        if (command.isActive() != null) asset.setActive(command.isActive());
        if (command.currentValue() != null) asset.setCurrentValue(command.currentValue());
        if (command.valuationDate() != null) asset.setValuationDate(command.valuationDate());

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
