package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import com.suchika.wealth.ports.input.CreatePhysicalAssetCommand;
import com.suchika.wealth.ports.input.PagedPhysicalAssets;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalAssetServiceTest {

    private PhysicalAssetService service;
    private FakePhysicalAssetRepository repo;

    @BeforeEach
    void setUp() {
        repo = new FakePhysicalAssetRepository();
        service = new PhysicalAssetService(repo);
    }

    private static CreatePhysicalAssetCommand cmd(String assetName, AssetType assetType, String make, String model,
                                                    String registrationNumber, RegistrationType registrationType) {
        return new CreatePhysicalAssetCommand(assetName, assetType, make, model, registrationNumber, registrationType);
    }

    @Test
    void createAsset_happyPath_returnsAssetWithId() {
        PhysicalAsset result = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        assertNotNull(result.getId());
        assertEquals("Family Car", result.getAssetName());
        assertEquals(AssetType.VEHICLE, result.getAssetType());
        assertEquals("KA-01-AB-1234", result.getRegistrationNumber());
        assertTrue(result.isActive());
    }

    @Test
    void createAsset_blankAssetName_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("  ", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_nullAssetType_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("Family Car", null, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_blankMake_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("Family Car", AssetType.VEHICLE, " ", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_blankModel_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("Family Car", AssetType.VEHICLE, "Maruti", " ", "KA-01-AB-1234", RegistrationType.PRIVATE);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_blankRegistrationNumber_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", " ", RegistrationType.PRIVATE);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_nullRegistrationType_throwsBadRequest() {
        CreatePhysicalAssetCommand command = cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", null);
        assertThrows(BadRequestException.class, () -> service.createAsset(null, command));
    }

    @Test
    void createAsset_duplicateRegistrationNumber_throwsConflict() {
        service.createAsset(null, cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));
        CreatePhysicalAssetCommand duplicate = cmd("Second Car", AssetType.VEHICLE, "Hyundai", "i20", "KA-01-AB-1234", RegistrationType.PRIVATE);

        assertThrows(ConflictException.class, () -> service.createAsset(null, duplicate));
    }

    @Test
    void getAsset_found_returnsAsset() {
        PhysicalAsset created = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        PhysicalAsset found = service.getAsset(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getAsset(randomId));
    }

    @Test
    void listAssets_filtersByTypeAndActive() {
        service.createAsset(null, cmd("Car 1", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));
        service.createAsset(null, cmd("Car 2", AssetType.VEHICLE, "Hyundai", "i20", "KA-01-AB-5678", RegistrationType.COMMERCIAL));

        assertEquals(2, service.listAssets(null, AssetType.VEHICLE, null).size());
        assertEquals(2, service.listAssets(null, null, true).size());
    }

    @Test
    void updateAsset_partialFields_updatesOnlyProvided() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        PhysicalAsset updated = service.updateAsset(asset.getId(), "Renamed Car", null, null, null, null);

        assertEquals("Renamed Car", updated.getAssetName());
        assertEquals("Maruti", updated.getMake());
    }

    @Test
    void updateAsset_blankAssetName_throwsBadRequest() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));
        UUID assetId = asset.getId();

        assertThrows(BadRequestException.class, () -> service.updateAsset(assetId, "  ", null, null, null, null));
    }

    @Test
    void updateAsset_metadata_mergesRatherThanOverwrites() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        service.updateAsset(asset.getId(), null, null, null, Map.of("puc_expiry", "2026-12-31"), null);
        PhysicalAsset updated = service.updateAsset(asset.getId(), null, null, null, Map.of("insurance_expiry", "2027-01-15"), null);

        assertEquals("2026-12-31", updated.getMetadata().get("puc_expiry"));
        assertEquals("2027-01-15", updated.getMetadata().get("insurance_expiry"));
    }

    @Test
    void updateAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.updateAsset(randomId, "Renamed", null, null, null, null));
    }

    @Test
    void deactivateAsset_setsInactive() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        service.deactivateAsset(asset.getId());

        assertFalse(service.getAsset(asset.getId()).isActive());
    }

    @Test
    void deactivateAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.deactivateAsset(randomId));
    }

    // ---- v1.0 pagination extension (Q54): physical asset list pagination ----

    @Test
    void listAssetsPaginated_returnsRequestedPageAndTotalCount() {
        for (int i = 0; i < 5; i++) {
            service.createAsset(null, cmd("Vehicle " + i, AssetType.VEHICLE, "Maruti", "Swift",
                    "KA-01-AB-000" + i, RegistrationType.PRIVATE));
        }

        PagedPhysicalAssets result = service.listAssetsPaginated(null, null, null, 1, 2);

        assertEquals(2, result.assets().size());
        assertEquals(5, result.totalCount());
        assertEquals(1, repo.lastPage);
        assertEquals(2, repo.lastSize);
    }

    @Test
    void listAssetsPaginated_passesFiltersThroughToRepo() {
        UUID profileId = UUID.randomUUID();

        service.listAssetsPaginated(profileId, AssetType.VEHICLE, true, 0, 10);

        assertEquals(profileId, repo.lastProfileId);
        assertEquals(AssetType.VEHICLE, repo.lastAssetType);
        assertEquals(true, repo.lastIsActive);
    }

    // ---- Fake repository ----

    static class FakePhysicalAssetRepository implements PhysicalAssetRepository {
        private final Map<UUID, PhysicalAsset> store = new HashMap<>();

        UUID lastProfileId;
        AssetType lastAssetType;
        Boolean lastIsActive;
        Integer lastPage;
        Integer lastSize;

        @Override
        public PhysicalAsset save(PhysicalAsset asset) {
            if (asset.getId() == null) {
                asset = PhysicalAsset.builder()
                        .id(UUID.randomUUID())
                        .profileId(asset.getProfileId())
                        .assetName(asset.getAssetName())
                        .assetType(asset.getAssetType())
                        .make(asset.getMake())
                        .model(asset.getModel())
                        .registrationNumber(asset.getRegistrationNumber())
                        .registrationType(asset.getRegistrationType())
                        .active(asset.isActive())
                        .metadata(asset.getMetadata())
                        .build();
            }
            store.put(asset.getId(), asset);
            return asset;
        }

        @Override
        public Optional<PhysicalAsset> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive) {
            this.lastProfileId = profileId;
            this.lastAssetType = assetType;
            this.lastIsActive = isActive;
            return store.values().stream()
                    .filter(a -> profileId == null || profileId.equals(a.getProfileId()))
                    .filter(a -> assetType == null || assetType == a.getAssetType())
                    .filter(a -> isActive == null || isActive.equals(a.isActive()))
                    .toList();
        }

        @Override
        public List<PhysicalAsset> findAll(UUID profileId, AssetType assetType, Boolean isActive, int page, int size) {
            this.lastPage = page;
            this.lastSize = size;
            List<PhysicalAsset> all = findAll(profileId, assetType, isActive);
            int start = Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            return all.subList(start, end);
        }

        @Override
        public long countAll(UUID profileId, AssetType assetType, Boolean isActive) {
            return findAll(profileId, assetType, isActive).size();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }

        @Override
        public boolean existsByRegistrationNumber(String registrationNumber) {
            return store.values().stream()
                    .anyMatch(a -> registrationNumber.equals(a.getRegistrationNumber()));
        }
    }
}
