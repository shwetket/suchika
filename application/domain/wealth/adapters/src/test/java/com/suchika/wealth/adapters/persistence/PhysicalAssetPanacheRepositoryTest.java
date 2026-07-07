package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PhysicalAssetPanacheRepository.
 * Requires a running local PostgreSQL (app_db).
 * Each test runs in a transaction that is rolled back on completion.
 */
@QuarkusTest
@TestProfile(PhysicalAssetPanacheRepositoryTest.DatabaseIntegrationProfile.class)
@TestTransaction
class PhysicalAssetPanacheRepositoryTest {

    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }
    }

    @Inject
    PhysicalAssetRepository repository;

    @Inject
    EntityManager em;

    @Test
    void save_andFindById_roundTrip() {
        UUID profileId = saveProfile("Owner Member");

        PhysicalAsset saved = repository.save(asset(profileId, "Family Car", "KA-01-AB-1234"));

        assertNotNull(saved.getId());
        assertEquals(profileId, saved.getProfileId());
        assertEquals("Family Car", saved.getAssetName());
        assertEquals("KA-01-AB-1234", saved.getRegistrationNumber());
        assertTrue(saved.isActive());

        Optional<PhysicalAsset> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Family Car", found.get().getAssetName());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    // ---- v1.0 pagination extension (Q54): physical asset list pagination ----

    @Test
    void findAll_paginated_partitionsFullResultSetWithNoOverlapOrGap() {
        UUID profileId = saveProfile("Pagination Owner");
        List<UUID> insertedIds = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PhysicalAsset saved = repository.save(asset(profileId, "Vehicle " + i, "KA-01-AB-000" + i));
            insertedIds.add(saved.getId());
        }

        List<PhysicalAsset> firstPage = repository.findAll(profileId, null, null, 0, 2);
        List<PhysicalAsset> secondPage = repository.findAll(profileId, null, null, 1, 2);
        List<PhysicalAsset> thirdPage = repository.findAll(profileId, null, null, 2, 2);

        assertEquals(2, firstPage.size());
        assertEquals(2, secondPage.size());
        assertEquals(1, thirdPage.size());

        List<UUID> pagedIds = java.util.stream.Stream.of(firstPage, secondPage, thirdPage)
                .flatMap(List::stream)
                .map(PhysicalAsset::getId)
                .toList();

        assertEquals(5, pagedIds.size(), "pages together must cover the whole result set exactly once");
        assertEquals(new java.util.HashSet<>(insertedIds), new java.util.HashSet<>(pagedIds),
                "paged results must be a partition of the inserted set, no duplicates or omissions");
    }

    @Test
    void countAll_returnsTotalMatchingFilterIgnoringPage() {
        UUID profileId = saveProfile("Count Owner");
        for (int i = 1; i <= 5; i++) {
            repository.save(asset(profileId, "Vehicle " + i, "KA-02-AB-000" + i));
        }

        long total = repository.countAll(profileId, null, null);

        assertEquals(5, total);
        // count must ignore page/size — a single page of 2 still reports the full total.
        assertEquals(2, repository.findAll(profileId, null, null, 0, 2).size());
    }

    @Test
    void countAll_appliesSameFiltersAsFindAll() {
        UUID profileId = saveProfile("Filtered Owner");
        repository.save(PhysicalAsset.builder()
                .profileId(profileId)
                .assetName("Active Car")
                .assetType(AssetType.VEHICLE)
                .make("Maruti")
                .model("Swift")
                .registrationNumber("KA-03-AB-0001")
                .registrationType(RegistrationType.PRIVATE)
                .active(true)
                .build());
        repository.save(PhysicalAsset.builder()
                .profileId(profileId)
                .assetName("Inactive Car")
                .assetType(AssetType.VEHICLE)
                .make("Hyundai")
                .model("i20")
                .registrationNumber("KA-03-AB-0002")
                .registrationType(RegistrationType.PRIVATE)
                .active(false)
                .build());

        assertEquals(1, repository.countAll(profileId, null, true));
        assertEquals(1, repository.findAll(profileId, null, true, 0, 50).size());
    }

    @Test
    void findAll_profileFilter_blocksCrossProfileAccess() {
        UUID ownerProfileId = saveProfile("Owner Member 2");
        UUID otherProfileId = saveProfile("Other Member 2");

        repository.save(asset(ownerProfileId, "Owner's Car", "KA-04-AB-0001"));

        List<PhysicalAsset> asOwner = repository.findAll(ownerProfileId, null, null, 0, 50);
        assertEquals(1, asOwner.size(), "Owner profile should see their own asset");

        List<PhysicalAsset> asOtherProfile = repository.findAll(otherProfileId, null, null, 0, 50);
        assertTrue(asOtherProfile.isEmpty(), "A different profile_id must not see another profile's assets");

        assertEquals(1, repository.countAll(ownerProfileId, null, null));
        assertEquals(0, repository.countAll(otherProfileId, null, null));
    }

    // ---- Helpers ----

    /**
     * Inserts a minimal profile.admin + profile.profile row pair via native SQL
     * so physical_asset.profile_id (NOT NULL, FK to profile.profile) is satisfiable
     * in tests. Rolled back automatically by @TestTransaction. Mirrors
     * TransactionPanacheRepositoryTest.saveProfile() since wealth's integration-test
     * Flyway locations only cover the wealth schema, not profile's.
     */
    private UUID saveProfile(String fullName) {
        UUID adminId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.admin (id, display_name) VALUES (?1, ?2)")
                .setParameter(1, adminId)
                .setParameter(2, "Test Admin")
                .executeUpdate();

        UUID profileId = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO profile.profile (id, admin_id, full_name, dob, relation_to_admin) "
                        + "VALUES (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, profileId)
                .setParameter(2, adminId)
                .setParameter(3, fullName)
                .setParameter(4, LocalDate.of(1990, Month.JANUARY, 1))
                .setParameter(5, "OTHER")
                .executeUpdate();

        return profileId;
    }

    private PhysicalAsset asset(UUID profileId, String assetName, String registrationNumber) {
        return PhysicalAsset.builder()
                .profileId(profileId)
                .assetName(assetName)
                .assetType(AssetType.VEHICLE)
                .make("Maruti")
                .model("Swift")
                .registrationNumber(registrationNumber)
                .registrationType(RegistrationType.PRIVATE)
                .build();
    }
}
