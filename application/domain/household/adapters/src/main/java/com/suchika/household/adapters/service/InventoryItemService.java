package com.suchika.household.adapters.service;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.input.InventoryItemUseCase;
import com.suchika.household.ports.output.InventoryItemRepository;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class InventoryItemService implements InventoryItemUseCase {

    private static final String ITEM_NOT_FOUND = "Inventory item not found: ";

    private final InventoryItemRepository repository;

    public InventoryItemService(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InventoryItem create(UUID profileId, String itemName, BigDecimal quantity,
                                ItemUnit unit, SourcePlatform sourcePlatform,
                                LocalDate purchaseDate, String category) {
        InventoryItem item = InventoryItem.create(profileId, itemName, quantity,
                unit, sourcePlatform, purchaseDate, category);
        InventoryItem saved = repository.save(item);
        AppLogger.info("Inventory item created: %s for profile: %s", saved.getId(), profileId);
        return saved;
    }

    @Override
    public List<InventoryItem> list(UUID profileId, SourcePlatform sourcePlatform, String category) {
        return repository.findByProfileId(profileId, sourcePlatform, category);
    }

    @Override
    public InventoryItem get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ITEM_NOT_FOUND + id));
    }

    @Override
    @Transactional
    public InventoryItem update(UUID id, String itemName, BigDecimal quantity, ItemUnit unit,
                                SourcePlatform sourcePlatform, LocalDate purchaseDate,
                                String category, Boolean isConsumed) {
        InventoryItem existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(ITEM_NOT_FOUND + id));

        InventoryItem updated = InventoryItem.builder()
                .id(existing.getId())
                .profileId(existing.getProfileId())
                .itemName(itemName != null ? itemName : existing.getItemName())
                .quantity(quantity != null ? quantity : existing.getQuantity())
                .unit(unit != null ? unit : existing.getUnit())
                .sourcePlatform(sourcePlatform != null ? sourcePlatform : existing.getSourcePlatform())
                .purchaseDate(purchaseDate != null ? purchaseDate : existing.getPurchaseDate())
                .category(category != null ? category : existing.getCategory())
                .consumed(isConsumed != null ? isConsumed : existing.isConsumed())
                .createdAt(existing.getCreatedAt())
                .build();

        InventoryItem saved = repository.save(updated);
        AppLogger.info("Inventory item updated: %s", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(ITEM_NOT_FOUND + id);
        }
        repository.deleteById(id);
        AppLogger.info("Inventory item deleted: %s", id);
    }
}
