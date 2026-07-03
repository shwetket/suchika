package com.suchika.household.adapters.persistence;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.output.InventoryItemRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InventoryItemPanacheRepository.
 * Requires a running local PostgreSQL (app_db) with household schema and test seed data.
 * Profile seed (R__seed_profile_test_data.sql) must have run before these tests.
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestProfile(InventoryItemPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class InventoryItemPanacheRepositoryTest {

    /**
     * Activates the 'integration-test' Quarkus config profile which re-enables
     * the datasource. The default 'test' profile disables the DB for @InjectMock tests.
     */
    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }

    // Seeded by R__seed_profile_test_data.sql via profile domain migrations
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // A second distinct UUID for profile-isolation tests — has no rows seeded for it
    private static final UUID OTHER_PROFILE_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099");

    @Inject
    InventoryItemRepository repository;

    @Inject
    EntityManager em;

    // --- save + findById ---

    @Test
    void save_andFindById_roundTrip() {
        InventoryItem saved = repository.save(item("Basmati Rice", new BigDecimal("5.000"),
                ItemUnit.KG, SourcePlatform.BLINKIT, LocalDate.of(2026, Month.JULY, 1), "Grains"));

        assertNotNull(saved.getId());
        assertEquals(SEED_PROFILE_ID, saved.getProfileId());
        assertEquals("Basmati Rice", saved.getItemName());
        assertEquals(0, new BigDecimal("5.000").compareTo(saved.getQuantity()));
        assertEquals(ItemUnit.KG, saved.getUnit());
        assertEquals(SourcePlatform.BLINKIT, saved.getSourcePlatform());
        assertEquals(LocalDate.of(2026, Month.JULY, 1), saved.getPurchaseDate());
        assertEquals("Grains", saved.getCategory());
        assertNotNull(saved.getCreatedAt());

        Optional<InventoryItem> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Basmati Rice", found.get().getItemName());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    // --- findByProfileId filters ---

    @Test
    void findByProfileId_noFilters_returnsAllForProfile() {
        repository.save(item("Milk", new BigDecimal("2.000"), ItemUnit.L,
                SourcePlatform.INSTAMART, LocalDate.of(2026, Month.AUGUST, 1), null));
        repository.save(item("Bread", new BigDecimal("1.000"), ItemUnit.UNITS,
                SourcePlatform.MANUAL, LocalDate.of(2026, Month.AUGUST, 2), "Bakery"));

        List<InventoryItem> results = repository.findByProfileId(SEED_PROFILE_ID, null, null);

        assertTrue(results.size() >= 2);
        assertTrue(results.stream().anyMatch(i -> "Milk".equals(i.getItemName())));
        assertTrue(results.stream().anyMatch(i -> "Bread".equals(i.getItemName())));
    }

    @Test
    void findByProfileId_sourcePlatformFilter_returnsOnlyMatchingPlatform() {
        repository.save(item("Apples", new BigDecimal("1.500"), ItemUnit.KG,
                SourcePlatform.BLINKIT, LocalDate.of(2026, Month.SEPTEMBER, 1), "Fruit"));
        repository.save(item("Onions", new BigDecimal("2.000"), ItemUnit.KG,
                SourcePlatform.ZEPTO, LocalDate.of(2026, Month.SEPTEMBER, 2), "Vegetables"));

        List<InventoryItem> blinkitOnly = repository.findByProfileId(SEED_PROFILE_ID, SourcePlatform.BLINKIT, null);

        assertTrue(blinkitOnly.stream().allMatch(i -> i.getSourcePlatform() == SourcePlatform.BLINKIT));
        assertTrue(blinkitOnly.stream().anyMatch(i -> "Apples".equals(i.getItemName())));
        assertTrue(blinkitOnly.stream().noneMatch(i -> "Onions".equals(i.getItemName())));
    }

    @Test
    void findByProfileId_categoryFilter_returnsOnlyMatchingCategory() {
        repository.save(item("Almonds", new BigDecimal("0.500"), ItemUnit.KG,
                SourcePlatform.AMAZON_FRESH, LocalDate.of(2026, Month.OCTOBER, 1), "Nuts"));
        repository.save(item("Cashews", new BigDecimal("0.250"), ItemUnit.KG,
                SourcePlatform.AMAZON_FRESH, LocalDate.of(2026, Month.OCTOBER, 2), "Nuts"));
        repository.save(item("Mango", new BigDecimal("1.000"), ItemUnit.KG,
                SourcePlatform.AMAZON_FRESH, LocalDate.of(2026, Month.OCTOBER, 3), "Fruit"));

        List<InventoryItem> nutsOnly = repository.findByProfileId(SEED_PROFILE_ID, null, "Nuts");

        assertTrue(nutsOnly.stream().allMatch(i -> "Nuts".equals(i.getCategory())));
        assertTrue(nutsOnly.stream().anyMatch(i -> "Almonds".equals(i.getItemName())));
        assertTrue(nutsOnly.stream().anyMatch(i -> "Cashews".equals(i.getItemName())));
        assertTrue(nutsOnly.stream().noneMatch(i -> "Mango".equals(i.getItemName())));
    }

    @Test
    void findByProfileId_combinedFilter_platformAndCategory() {
        repository.save(item("Premium Rice", new BigDecimal("5.000"), ItemUnit.KG,
                SourcePlatform.BLINKIT, LocalDate.of(2026, Month.NOVEMBER, 1), "Grains"));
        repository.save(item("Atta Flour", new BigDecimal("10.000"), ItemUnit.KG,
                SourcePlatform.BLINKIT, LocalDate.of(2026, Month.NOVEMBER, 2), "Grains"));
        repository.save(item("Chips", new BigDecimal("1.000"), ItemUnit.PACK,
                SourcePlatform.BLINKIT, LocalDate.of(2026, Month.NOVEMBER, 3), "Snacks"));
        repository.save(item("Lentils", new BigDecimal("2.000"), ItemUnit.KG,
                SourcePlatform.ZEPTO, LocalDate.of(2026, Month.NOVEMBER, 4), "Grains"));

        List<InventoryItem> results = repository.findByProfileId(SEED_PROFILE_ID, SourcePlatform.BLINKIT, "Grains");

        assertTrue(results.stream().allMatch(i -> i.getSourcePlatform() == SourcePlatform.BLINKIT));
        assertTrue(results.stream().allMatch(i -> "Grains".equals(i.getCategory())));
        assertTrue(results.stream().anyMatch(i -> "Premium Rice".equals(i.getItemName())));
        assertTrue(results.stream().anyMatch(i -> "Atta Flour".equals(i.getItemName())));
        assertTrue(results.stream().noneMatch(i -> "Chips".equals(i.getItemName())));
        assertTrue(results.stream().noneMatch(i -> "Lentils".equals(i.getItemName())));
    }

    @Test
    void findByProfileId_orderedByPurchaseDateDesc() {
        repository.save(item("Oldest Item", new BigDecimal("1.000"), ItemUnit.KG,
                SourcePlatform.MANUAL, LocalDate.of(2026, Month.JANUARY, 1), null));
        repository.save(item("Middle Item", new BigDecimal("1.000"), ItemUnit.KG,
                SourcePlatform.MANUAL, LocalDate.of(2026, Month.JUNE, 15), null));
        repository.save(item("Newest Item", new BigDecimal("1.000"), ItemUnit.KG,
                SourcePlatform.MANUAL, LocalDate.of(2026, Month.DECEMBER, 31), null));

        List<InventoryItem> results = repository.findByProfileId(SEED_PROFILE_ID, SourcePlatform.MANUAL, null);

        List<LocalDate> dates = results.stream()
                .map(InventoryItem::getPurchaseDate)
                .toList();
        for (int i = 0; i < dates.size() - 1; i++) {
            assertFalse(dates.get(i).isBefore(dates.get(i + 1)),
                    "Expected DESC order but found " + dates.get(i) + " before " + dates.get(i + 1));
        }
    }

    // --- profile-scoped isolation ---

    @Test
    void findByProfileId_doesNotLeakAcrossProfiles() {
        repository.save(item("Profile A Item", new BigDecimal("1.000"), ItemUnit.KG,
                SourcePlatform.BLINKIT, LocalDate.of(2026, Month.AUGUST, 10), null));

        // OTHER_PROFILE_ID has no FK row in profile.profile — querying it returns empty
        List<InventoryItem> otherResults = repository.findByProfileId(OTHER_PROFILE_ID, null, null);

        assertTrue(otherResults.isEmpty());
    }

    // --- save update ---

    @Test
    void save_update_persistsNewQuantityAndCategory() {
        InventoryItem saved = repository.save(item("Sugar", new BigDecimal("1.000"),
                ItemUnit.KG, SourcePlatform.BLINKIT, LocalDate.of(2026, Month.AUGUST, 1), "Sweeteners"));

        InventoryItem updated = InventoryItem.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .itemName("Sugar")
                .quantity(new BigDecimal("2.000"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.BLINKIT)
                .purchaseDate(LocalDate.of(2026, Month.AUGUST, 1))
                .category("Baking")
                .createdAt(saved.getCreatedAt())
                .build();

        InventoryItem result = repository.save(updated);

        assertEquals(0, new BigDecimal("2.000").compareTo(result.getQuantity()));
        assertEquals("Baking", result.getCategory());
    }

    @Test
    void save_defaultsIsConsumedToFalse() {
        InventoryItem saved = repository.save(item("Tea Leaves", new BigDecimal("0.250"),
                ItemUnit.KG, SourcePlatform.MANUAL, LocalDate.of(2026, Month.AUGUST, 5), "Beverages"));

        assertFalse(saved.isConsumed());
        assertFalse(repository.findById(saved.getId()).orElseThrow().isConsumed());
    }

    @Test
    void save_update_toggleIsConsumedTrue_persists() {
        InventoryItem saved = repository.save(item("Coffee", new BigDecimal("0.500"),
                ItemUnit.KG, SourcePlatform.MANUAL, LocalDate.of(2026, Month.AUGUST, 6), "Beverages"));

        InventoryItem updated = InventoryItem.builder()
                .id(saved.getId())
                .profileId(SEED_PROFILE_ID)
                .itemName(saved.getItemName())
                .quantity(saved.getQuantity())
                .unit(saved.getUnit())
                .sourcePlatform(saved.getSourcePlatform())
                .purchaseDate(saved.getPurchaseDate())
                .category(saved.getCategory())
                .consumed(true)
                .createdAt(saved.getCreatedAt())
                .build();

        InventoryItem result = repository.save(updated);

        assertTrue(result.isConsumed());
        assertTrue(repository.findById(saved.getId()).orElseThrow().isConsumed());
    }

    // --- delete ---

    @Test
    void deleteById_removesItem() {
        InventoryItem saved = repository.save(item("Disposable Item", new BigDecimal("1.000"),
                ItemUnit.PACK, SourcePlatform.MANUAL, LocalDate.of(2026, Month.DECEMBER, 1), null));
        UUID id = saved.getId();
        assertTrue(repository.existsById(id));

        repository.deleteById(id);

        assertFalse(repository.existsById(id));
        assertTrue(repository.findById(id).isEmpty());
    }

    // --- existsById ---

    @Test
    void existsById_falseForUnknownId() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    @Test
    void existsById_trueAfterSave() {
        InventoryItem saved = repository.save(item("Exists Check Item", new BigDecimal("1.000"),
                ItemUnit.UNITS, SourcePlatform.MANUAL, LocalDate.of(2026, Month.AUGUST, 20), null));
        assertTrue(repository.existsById(saved.getId()));
    }

    // --- DB constraint guard ---

    @Test
    void save_zeroQuantity_throwsConstraintViolation() {
        // DB CHECK constraint: chk_quantity_positive — quantity > 0
        InventoryItem invalid = InventoryItem.builder()
                .profileId(SEED_PROFILE_ID)
                .itemName("Zero Qty Item")
                .quantity(new BigDecimal("0.000"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.MANUAL)
                .purchaseDate(LocalDate.of(2026, Month.AUGUST, 1))
                .build();

        assertThrows(Exception.class, () -> {
            repository.save(invalid);
            em.flush();
        }, "Expected constraint violation for quantity = 0");
    }

    // ---- helper ----

    private InventoryItem item(String name, BigDecimal quantity, ItemUnit unit,
                               SourcePlatform platform, LocalDate purchaseDate, String category) {
        return InventoryItem.builder()
                .profileId(SEED_PROFILE_ID)
                .itemName(name)
                .quantity(quantity)
                .unit(unit)
                .sourcePlatform(platform)
                .purchaseDate(purchaseDate)
                .category(category)
                .build();
    }
}
