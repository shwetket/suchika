package com.suchika.household.adapters.service;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.input.PagedInventoryItems;
import com.suchika.household.ports.input.UpdateInventoryItemCommand;
import com.suchika.household.ports.output.InventoryItemRepository;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryItemServiceTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, Month.JUNE, 1);

    private StubInventoryItemRepository repository;
    private InventoryItemService service;

    @BeforeEach
    void setUp() {
        repository = new StubInventoryItemRepository();
        service = new InventoryItemService(repository);
    }

    @Test
    void create_happyPath_returnsItemWithId() {
        UUID profileId = UUID.randomUUID();
        InventoryItem item = service.create(profileId, "Amul Milk", new BigDecimal("2"),
                ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, "Dairy");

        assertNotNull(item.getId());
        assertEquals(profileId, item.getProfileId());
        assertEquals("Amul Milk", item.getItemName());
        assertEquals(SourcePlatform.INSTAMART, item.getSourcePlatform());
    }

    @Test
    void list_filterBySourcePlatform_returnsOnlyMatchingItems() {
        UUID profileId = UUID.randomUUID();
        service.create(profileId, "Milk", new BigDecimal("2"), ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, null);
        service.create(profileId, "Rice", new BigDecimal("5"), ItemUnit.KG, SourcePlatform.BLINKIT, PURCHASE_DATE, null);

        List<InventoryItem> results = service.list(profileId, SourcePlatform.INSTAMART, null);

        assertEquals(1, results.size());
        assertEquals("Milk", results.get(0).getItemName());
    }

    @Test
    void list_filterByCategory_returnsMatchingItems() {
        UUID profileId = UUID.randomUUID();
        service.create(profileId, "Milk", new BigDecimal("2"), ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, "Dairy");
        service.create(profileId, "Rice", new BigDecimal("5"), ItemUnit.KG, SourcePlatform.BLINKIT, PURCHASE_DATE, "Grains");

        List<InventoryItem> results = service.list(profileId, null, "Dairy");

        assertEquals(1, results.size());
        assertEquals("Milk", results.get(0).getItemName());
    }

    // ---- Q54 pagination pass ----

    @Test
    void listPaginated_returnsRequestedPageAndTotalCount() {
        UUID profileId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            service.create(profileId, "Item " + i, new BigDecimal("1"),
                    ItemUnit.UNITS, SourcePlatform.MANUAL, PURCHASE_DATE, null);
        }

        PagedInventoryItems result = service.listPaginated(profileId, null, null, 1, 2);

        assertEquals(2, result.items().size());
        assertEquals(5, result.totalCount());
    }

    @Test
    void listPaginated_passesFiltersThroughToRepo() {
        UUID profileId = UUID.randomUUID();
        service.create(profileId, "Milk", new BigDecimal("2"), ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, null);
        service.create(profileId, "Rice", new BigDecimal("5"), ItemUnit.KG, SourcePlatform.BLINKIT, PURCHASE_DATE, null);

        PagedInventoryItems result = service.listPaginated(profileId, SourcePlatform.INSTAMART, null, 0, 10);

        assertEquals(1, result.items().size());
        assertEquals(1, result.totalCount());
        assertEquals("Milk", result.items().get(0).getItemName());
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void get_found_returnsItem() {
        UUID profileId = UUID.randomUUID();
        InventoryItem created = service.create(profileId, "Curd", new BigDecimal("1"),
                ItemUnit.KG, SourcePlatform.MANUAL, PURCHASE_DATE, null);

        InventoryItem found = service.get(created.getId());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.update(UUID.randomUUID(),
                new UpdateInventoryItemCommand("New Name", null, null, null, null, null, null)));
    }

    @Test
    void update_patchesProvidedFieldsOnly() {
        UUID profileId = UUID.randomUUID();
        InventoryItem created = service.create(profileId, "Milk", new BigDecimal("2"),
                ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, "Dairy");

        InventoryItem updated = service.update(created.getId(),
                new UpdateInventoryItemCommand("Whole Milk", null, null, null, null, null, null));

        assertEquals("Whole Milk", updated.getItemName());
        assertEquals(0, new BigDecimal("2").compareTo(updated.getQuantity()));
        assertEquals(ItemUnit.L, updated.getUnit());
        assertEquals(SourcePlatform.INSTAMART, updated.getSourcePlatform());
        assertEquals("Dairy", updated.getCategory());
        assertEquals(false, updated.isConsumed());
    }

    @Test
    void update_toggleIsConsumedTrue_marksRecordConsumed() {
        UUID profileId = UUID.randomUUID();
        InventoryItem created = service.create(profileId, "Milk", new BigDecimal("2"),
                ItemUnit.L, SourcePlatform.INSTAMART, PURCHASE_DATE, "Dairy");
        assertEquals(false, created.isConsumed());

        InventoryItem updated = service.update(created.getId(),
                new UpdateInventoryItemCommand(null, null, null, null, null, null, true));

        assertEquals(true, updated.isConsumed());
        // Untouched fields remain the same — is_consumed means "used in a calculation",
        // not "used up"; no other data should be altered by the toggle.
        assertEquals("Milk", updated.getItemName());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
    }

    @Test
    void delete_existing_removesItem() {
        UUID profileId = UUID.randomUUID();
        InventoryItem created = service.create(profileId, "Butter", new BigDecimal("0.5"),
                ItemUnit.KG, SourcePlatform.COUNTRY_DELIGHT, PURCHASE_DATE, null);

        service.delete(created.getId());

        assertThrows(NotFoundException.class, () -> service.get(created.getId()));
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubInventoryItemRepository implements InventoryItemRepository {

        private final Map<UUID, InventoryItem> store = new LinkedHashMap<>();

        @Override
        public InventoryItem save(InventoryItem item) {
            UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
            InventoryItem stored = InventoryItem.builder()
                    .id(id)
                    .profileId(item.getProfileId())
                    .itemName(item.getItemName())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .sourcePlatform(item.getSourcePlatform())
                    .purchaseDate(item.getPurchaseDate())
                    .category(item.getCategory())
                    .consumed(item.isConsumed())
                    .createdAt(item.getCreatedAt() != null ? item.getCreatedAt() : Instant.EPOCH)
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<InventoryItem> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category) {
            return store.values().stream()
                    .filter(i -> i.getProfileId().equals(profileId))
                    .filter(i -> sourcePlatform == null || i.getSourcePlatform() == sourcePlatform)
                    .filter(i -> category == null || category.isBlank() || category.equals(i.getCategory()))
                    .toList();
        }

        @Override
        public List<InventoryItem> findByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category,
                                                    int page, int size) {
            List<InventoryItem> all = findByProfileId(profileId, sourcePlatform, category);
            int start = Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            return all.subList(start, end);
        }

        @Override
        public long countByProfileId(UUID profileId, SourcePlatform sourcePlatform, String category) {
            return findByProfileId(profileId, sourcePlatform, category).size();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }
    }
}
