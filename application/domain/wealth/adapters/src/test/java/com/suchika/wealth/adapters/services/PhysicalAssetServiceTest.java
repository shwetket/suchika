package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import com.suchika.wealth.ports.input.CreatePhysicalAssetCommand;
import com.suchika.wealth.ports.input.PagedPhysicalAssets;
import com.suchika.wealth.ports.input.UpdatePhysicalAssetCommand;
import com.suchika.wealth.ports.output.PhysicalAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
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
        return new CreatePhysicalAssetCommand(assetName, assetType, make, model, registrationNumber, registrationType,
                null, null);
    }

    private static CreatePhysicalAssetCommand cmdWithValuation(String assetName, AssetType assetType,
                                                                 BigDecimal currentValue, LocalDate valuationDate) {
        return new CreatePhysicalAssetCommand(assetName, assetType, null, null, null, null,
                currentValue, valuationDate);
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

    // ---- v1.0 net-worth-model pass: make/model/registration_number/registration_type
    // are genuinely optional now (the DB never had NOT NULL on these) — only make sense
    // for AssetType.VEHICLE. Non-vehicle assets (REAL_ESTATE, GOLD_JEWELRY, GOLD_BOND)
    // must be creatable with none of the four set. ----

    @Test
    void createAsset_realEstate_noMakeModelRegistration_succeeds() {
        PhysicalAsset result = service.createAsset(null,
                cmdWithValuation("Self-Occupied Flat", AssetType.REAL_ESTATE,
                        new BigDecimal("9000000.00"), LocalDate.of(2026, Month.JUNE, 1)));

        assertNotNull(result.getId());
        assertEquals(AssetType.REAL_ESTATE, result.getAssetType());
        assertNull(result.getMake());
        assertNull(result.getModel());
        assertNull(result.getRegistrationNumber());
        assertNull(result.getRegistrationType());
        assertEquals(new BigDecimal("9000000.00"), result.getCurrentValue());
        assertEquals(LocalDate.of(2026, Month.JUNE, 1), result.getValuationDate());
    }

    @Test
    void createAsset_goldJewelry_noMakeModelRegistration_succeeds() {
        PhysicalAsset result = service.createAsset(null,
                cmdWithValuation("Gold Jewellery", AssetType.GOLD_JEWELRY,
                        new BigDecimal("500000.00"), LocalDate.of(2026, Month.JUNE, 1)));

        assertNotNull(result.getId());
        assertEquals(AssetType.GOLD_JEWELRY, result.getAssetType());
        assertNull(result.getRegistrationNumber());
    }

    @Test
    void createAsset_blankRegistrationNumber_uniquenessCheckSkipped() {
        // Two assets both with a blank/absent registration_number must NOT collide —
        // the existsByRegistrationNumber guard only runs when registrationNumber is
        // non-blank (Postgres itself allows multiple NULLs under a UNIQUE constraint).
        service.createAsset(null, cmdWithValuation("Flat 1", AssetType.REAL_ESTATE, null, null));

        assertDoesNotThrow(() ->
                service.createAsset(null, cmdWithValuation("Flat 2", AssetType.REAL_ESTATE, null, null)));
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

        PhysicalAsset found = service.getAsset(created.getId(), null);

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getAsset(randomId, null));
    }

    @Test
    void getAsset_wrongProfile_throwsNotFoundException() {
        UUID ownerProfileId = UUID.randomUUID();
        UUID otherProfileId = UUID.randomUUID();
        PhysicalAsset created = service.createAsset(ownerProfileId,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        assertThrows(NotFoundException.class, () -> service.getAsset(created.getId(), otherProfileId));
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

        PhysicalAsset updated = service.updateAsset(asset.getId(), null,
                new UpdatePhysicalAssetCommand("Renamed Car", null, null, null, null, null, null));

        assertEquals("Renamed Car", updated.getAssetName());
        assertEquals("Maruti", updated.getMake());
    }

    @Test
    void updateAsset_blankAssetName_throwsBadRequest() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));
        UUID assetId = asset.getId();

        assertThrows(BadRequestException.class,
                () -> service.updateAsset(assetId, null,
                        new UpdatePhysicalAssetCommand("  ", null, null, null, null, null, null)));
    }

    @Test
    void updateAsset_metadata_mergesRatherThanOverwrites() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        service.updateAsset(asset.getId(), null,
                new UpdatePhysicalAssetCommand(null, null, null, Map.of("puc_expiry", "2026-12-31"), null, null, null));
        PhysicalAsset updated = service.updateAsset(asset.getId(), null,
                new UpdatePhysicalAssetCommand(null, null, null, Map.of("insurance_expiry", "2027-01-15"), null, null, null));

        assertEquals("2026-12-31", updated.getMetadata().get("puc_expiry"));
        assertEquals("2027-01-15", updated.getMetadata().get("insurance_expiry"));
    }

    @Test
    void updateAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class,
                () -> service.updateAsset(randomId, null,
                        new UpdatePhysicalAssetCommand("Renamed", null, null, null, null, null, null)));
    }

    @Test
    void updateAsset_wrongProfile_throwsNotFoundException() {
        UUID ownerProfileId = UUID.randomUUID();
        UUID otherProfileId = UUID.randomUUID();
        PhysicalAsset asset = service.createAsset(ownerProfileId,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        assertThrows(NotFoundException.class,
                () -> service.updateAsset(asset.getId(), otherProfileId,
                        new UpdatePhysicalAssetCommand("Renamed", null, null, null, null, null, null)));
    }

    @Test
    void updateAsset_currentValueAndValuationDate_updatesLater() {
        PhysicalAsset asset = service.createAsset(null,
                cmdWithValuation("Self-Occupied Flat", AssetType.REAL_ESTATE,
                        new BigDecimal("9000000.00"), LocalDate.of(2026, Month.JANUARY, 1)));

        PhysicalAsset updated = service.updateAsset(asset.getId(), null,
                new UpdatePhysicalAssetCommand(null, null, null, null, null,
                        new BigDecimal("9500000.00"), LocalDate.of(2026, Month.JULY, 1)));

        assertEquals(new BigDecimal("9500000.00"), updated.getCurrentValue());
        assertEquals(LocalDate.of(2026, Month.JULY, 1), updated.getValuationDate());
    }

    @Test
    void deactivateAsset_setsInactive() {
        PhysicalAsset asset = service.createAsset(null,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        service.deactivateAsset(asset.getId(), null);

        assertFalse(service.getAsset(asset.getId(), null).isActive());
    }

    @Test
    void deactivateAsset_notFound_throwsNotFoundException() {
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.deactivateAsset(randomId, null));
    }

    @Test
    void deactivateAsset_wrongProfile_throwsNotFoundException() {
        UUID ownerProfileId = UUID.randomUUID();
        UUID otherProfileId = UUID.randomUUID();
        PhysicalAsset asset = service.createAsset(ownerProfileId,
                cmd("Family Car", AssetType.VEHICLE, "Maruti", "Swift", "KA-01-AB-1234", RegistrationType.PRIVATE));

        assertThrows(NotFoundException.class, () -> service.deactivateAsset(asset.getId(), otherProfileId));
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
                        .currentValue(asset.getCurrentValue())
                        .valuationDate(asset.getValuationDate())
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
        public Optional<PhysicalAsset> findById(UUID id, UUID profileId) {
            PhysicalAsset asset = store.get(id);
            if (asset == null) return Optional.empty();
            // null profileId in tests means "no scoping requested" — permissive, matching
            // the many pre-existing tests that don't exercise profile isolation. A non-null
            // profileId must match the stored asset's profileId exactly (cross-profile test).
            if (profileId != null && !profileId.equals(asset.getProfileId())) return Optional.empty();
            return Optional.of(asset);
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
