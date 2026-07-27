package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.output.InventoryItemRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InventoryItemPanacheRepository implements InventoryItemRepository {

    private final InventoryItemDao dao;

    public InventoryItemPanacheRepository(InventoryItemDao dao) {
        this.dao = dao;
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemEntity entity = InventoryItemEntity.from(item);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<InventoryItem> findById(UUID id) {
        return dao.findByIdOptional(id).map(InventoryItemEntity::toDomain);
    }

    @Override
    public List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category) {
        String query = buildQuery(sourcePlatform, category);
        List<Object> params = buildParams(profileId, sourcePlatform, category);
        return dao.find(query, params.toArray())
                .stream()
                .map(InventoryItemEntity::toDomain)
                .toList();
    }

    @Override
    public List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category,
                                                int page, int size) {
        String query = buildQuery(sourcePlatform, category);
        List<Object> params = buildParams(profileId, sourcePlatform, category);
        return dao.find(query, params.toArray())
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(InventoryItemEntity::toDomain)
                .toList();
    }

    @Override
    public long countByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category) {
        String query = buildQuery(sourcePlatform, category);
        List<Object> params = buildParams(profileId, sourcePlatform, category);
        return dao.find(query, params.toArray()).count();
    }

    @Override
    public void deleteById(UUID id) {
        dao.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.count("id = ?1", id) > 0;
    }

    private String buildQuery(SourcePlatform sourcePlatform, String category) {
        StringBuilder sb = new StringBuilder("profileId = ?1");
        int idx = 2;
        if (sourcePlatform != null) { sb.append(" AND sourcePlatform = ?").append(idx++); }
        if (category != null && !category.isBlank()) { sb.append(" AND category = ?").append(idx); }
        sb.append(" ORDER BY purchaseDate DESC");
        return sb.toString();
    }

    private List<Object> buildParams(UUID profileId, SourcePlatform sourcePlatform, String category) {
        List<Object> params = new ArrayList<>();
        params.add(profileId);
        if (sourcePlatform != null) params.add(sourcePlatform.name());
        if (category != null && !category.isBlank()) params.add(category);
        return params;
    }
}
