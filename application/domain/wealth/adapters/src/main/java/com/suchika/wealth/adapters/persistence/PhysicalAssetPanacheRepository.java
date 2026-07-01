package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PhysicalAssetPanacheRepository implements PhysicalAssetRepository {

    private final PhysicalAssetDao dao;

    public PhysicalAssetPanacheRepository(PhysicalAssetDao dao) {
        this.dao = dao;
    }

    @Override
    public PhysicalAsset save(PhysicalAsset asset) {
        PhysicalAssetEntity entity = PhysicalAssetEntity.from(asset);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<PhysicalAsset> findById(UUID id) {
        return dao.findByIdOptional(id).map(PhysicalAssetEntity::toDomain);
    }

    @Override
    public List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive) {
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();

        if (profileId != null) {
            query.append("profileId = ?").append(params.size() + 1);
            params.add(profileId);
        }
        if (assetType != null) {
            if (!query.isEmpty()) query.append(" and ");
            query.append("assetType = ?").append(params.size() + 1);
            params.add(assetType.name());
        }
        if (isActive != null) {
            if (!query.isEmpty()) query.append(" and ");
            query.append("active = ?").append(params.size() + 1);
            params.add(isActive);
        }

        if (query.isEmpty()) {
            return dao.listAll().stream().map(PhysicalAssetEntity::toDomain).toList();
        }
        return dao.find(query.toString(), params.toArray())
                .stream().map(PhysicalAssetEntity::toDomain).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.findByIdOptional(id).isPresent();
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return dao.count("registrationNumber", registrationNumber) > 0;
    }
}
