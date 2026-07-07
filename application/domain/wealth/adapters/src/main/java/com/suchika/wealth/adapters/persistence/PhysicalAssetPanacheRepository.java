package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import com.suchika.shared.persistence.PanacheQueryFilter;

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
        Filter filter = buildFilter(profileId, assetType, isActive);
        return dao.find(filter.query(), filter.params().toArray())
                .stream().map(PhysicalAssetEntity::toDomain).toList();
    }

    @Override
    public List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive, int page, int size) {
        Filter filter = buildFilter(profileId, assetType, isActive);
        return dao.find(filter.query(), filter.params().toArray())
                .page(Page.of(page, size))
                .list()
                .stream().map(PhysicalAssetEntity::toDomain).toList();
    }

    @Override
    public long countAll(UUID profileId, AssetType assetType, Boolean isActive) {
        Filter filter = buildFilter(profileId, assetType, isActive);
        return dao.find(filter.query(), filter.params().toArray()).count();
    }

    /**
     * Shared predicate builder for {@code findAll} (both variants) and
     * {@code countAll} — keeps the filter logic in exactly one place (Sonar CPD)
     * now that there are three call sites needing the same predicate. Always
     * starts from a tautology so every optional filter can be appended uniformly
     * with "and", and orders newest-first so pagination across calls is stable.
     */
    private Filter buildFilter(UUID profileId, AssetType assetType, Boolean isActive) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (profileId != null) {
            query.append(" and profileId = ?").append(params.size() + 1);
            params.add(profileId);
        }
        if (assetType != null) {
            query.append(" and assetType = ?").append(params.size() + 1);
            params.add(assetType.name());
        }
        if (isActive != null) {
            query.append(" and active = ?").append(params.size() + 1);
            params.add(isActive);
        }
        query.append(" order by createdAt desc");

        return new Filter(query.toString(), params);
    }

    private record Filter(String query, List<Object> params) {
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
