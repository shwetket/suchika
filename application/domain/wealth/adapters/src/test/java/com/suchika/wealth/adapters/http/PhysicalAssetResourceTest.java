package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.CreatePhysicalAssetRequest;
import com.suchika.wealth.adapters.http.dto.ListPhysicalAssetsResponse;
import com.suchika.wealth.adapters.http.dto.PhysicalAssetResponse;
import com.suchika.wealth.adapters.http.dto.UpdatePhysicalAssetRequest;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.PhysicalAsset;
import com.suchika.wealth.domain.RegistrationType;
import com.suchika.wealth.ports.input.CreatePhysicalAssetCommand;
import com.suchika.wealth.ports.input.PagedPhysicalAssets;
import com.suchika.wealth.ports.input.PhysicalAssetUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhysicalAssetResourceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID ASSET_ID = UUID.randomUUID();

    private PhysicalAssetResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new PhysicalAssetResource(useCase);
    }

    @Test
    void listAssets_returns200_withAssetList() {
        useCase.assetsToReturn = List.of(buildAsset());
        useCase.totalCountToReturn = 1;

        Response response = resource.listAssets("VEHICLE", true, PROFILE_ID, null, null);

        assertEquals(200, response.getStatus());
        ListPhysicalAssetsResponse body = (ListPhysicalAssetsResponse) response.getEntity();
        assertEquals(1, body.physicalAssets.size());
        assertEquals(1, body.totalSize);
        assertEquals("Tata Nexon", body.physicalAssets.get(0).assetName);
        assertEquals(PROFILE_ID, useCase.lastListProfileId);
        assertEquals(AssetType.VEHICLE, useCase.lastListAssetType);
        assertEquals(true, useCase.lastListIsActive);
    }

    @Test
    void listAssets_invalidAssetType_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listAssets("NOT_A_TYPE", true, PROFILE_ID, null, null));
    }

    @Test
    void listAssets_defaultsPageAndSizeWhenNotProvided() {
        useCase.assetsToReturn = List.of(buildAsset());

        Response response = resource.listAssets(null, null, PROFILE_ID, null, null);

        ListPhysicalAssetsResponse body = (ListPhysicalAssetsResponse) response.getEntity();
        assertEquals(0, body.page);
        assertEquals(50, body.size);
        assertEquals(0, useCase.lastListPage);
        assertEquals(50, useCase.lastListSize);
    }

    @Test
    void listAssets_honorsExplicitPageAndSize() {
        useCase.assetsToReturn = List.of(buildAsset());
        useCase.totalCountToReturn = 120;

        Response response = resource.listAssets(null, null, PROFILE_ID, 2, 25);

        ListPhysicalAssetsResponse body = (ListPhysicalAssetsResponse) response.getEntity();
        assertEquals(2, body.page);
        assertEquals(25, body.size);
        assertEquals(120, body.totalSize);
        assertEquals(2, useCase.lastListPage);
        assertEquals(25, useCase.lastListSize);
    }

    @Test
    void listAssets_negativePage_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listAssets(null, null, PROFILE_ID, -1, null));
    }

    @Test
    void listAssets_sizeExceedsMax_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listAssets(null, null, PROFILE_ID, null, 500));
    }

    @Test
    void listAssets_sizeZero_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listAssets(null, null, PROFILE_ID, null, 0));
    }

    @Test
    void createAsset_returns201_withCreatedAsset() {
        CreatePhysicalAssetRequest request = new CreatePhysicalAssetRequest();
        request.assetName = "Tata Nexon";
        request.assetType = "VEHICLE";
        request.registrationType = "PRIVATE";
        useCase.assetToReturn = buildAsset();

        Response response = resource.createAsset(PROFILE_ID, request);

        assertEquals(201, response.getStatus());
        assertEquals(AssetType.VEHICLE, useCase.lastCreateCommand.assetType());
        assertEquals(RegistrationType.PRIVATE, useCase.lastCreateCommand.registrationType());
        PhysicalAssetResponse body = (PhysicalAssetResponse) response.getEntity();
        assertEquals("Tata Nexon", body.assetName);
    }

    @Test
    void createAsset_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.createAsset(PROFILE_ID, null));
    }

    @Test
    void createAsset_missingAssetType_throwsBadRequest() {
        CreatePhysicalAssetRequest request = new CreatePhysicalAssetRequest();
        request.assetName = "No Type";
        request.registrationType = "PRIVATE";
        assertThrows(BadRequestException.class, () -> resource.createAsset(PROFILE_ID, request));
    }

    @Test
    void createAsset_missingRegistrationType_throwsBadRequest() {
        CreatePhysicalAssetRequest request = new CreatePhysicalAssetRequest();
        request.assetName = "No Registration Type";
        request.assetType = "VEHICLE";
        assertThrows(BadRequestException.class, () -> resource.createAsset(PROFILE_ID, request));
    }

    @Test
    void getAsset_returnsAssetResponse() {
        useCase.assetToReturn = buildAsset();

        PhysicalAssetResponse response = resource.getAsset(ASSET_ID);

        assertEquals("Tata Nexon", response.assetName);
    }

    @Test
    void updateAsset_returnsUpdatedAsset() {
        UpdatePhysicalAssetRequest request = new UpdatePhysicalAssetRequest();
        request.metadata = Map.of("puc_expiry", "2027-01-01");
        useCase.assetToReturn = buildAsset();

        PhysicalAssetResponse response = resource.updateAsset(ASSET_ID, request);

        assertEquals("Tata Nexon", response.assetName);
        assertEquals(ASSET_ID, useCase.lastUpdateAssetId);
        assertEquals("2027-01-01", useCase.lastUpdateMetadata.get("puc_expiry"));
    }

    @Test
    void updateAsset_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateAsset(ASSET_ID, null));
    }

    @Test
    void deactivateAsset_returns204() {
        Response response = resource.deactivateAsset(ASSET_ID);

        assertEquals(204, response.getStatus());
        assertEquals(ASSET_ID, useCase.lastDeactivateAssetId);
    }

    private PhysicalAsset buildAsset() {
        return PhysicalAsset.builder()
                .id(ASSET_ID)
                .profileId(PROFILE_ID)
                .assetName("Tata Nexon")
                .assetType(AssetType.VEHICLE)
                .registrationType(RegistrationType.PRIVATE)
                .active(true)
                .build();
    }

    static class StubUseCase implements PhysicalAssetUseCase {
        List<PhysicalAsset> assetsToReturn = List.of();
        long totalCountToReturn;
        PhysicalAsset assetToReturn;

        CreatePhysicalAssetCommand lastCreateCommand;
        UUID lastUpdateAssetId;
        Map<String, String> lastUpdateMetadata;
        UUID lastDeactivateAssetId;

        UUID lastListProfileId;
        AssetType lastListAssetType;
        Boolean lastListIsActive;
        Integer lastListPage;
        Integer lastListSize;

        @Override
        public PhysicalAsset createAsset(UUID profileId, CreatePhysicalAssetCommand command) {
            lastCreateCommand = command;
            return assetToReturn;
        }

        @Override
        public PhysicalAsset getAsset(UUID id) {
            return assetToReturn;
        }

        @Override
        public List<PhysicalAsset> listAssets(UUID profileId, AssetType assetType, Boolean isActive) {
            return assetsToReturn;
        }

        @Override
        public PagedPhysicalAssets listAssetsPaginated(UUID profileId, AssetType assetType, Boolean isActive,
                                                         int page, int size) {
            lastListProfileId = profileId;
            lastListAssetType = assetType;
            lastListIsActive = isActive;
            lastListPage = page;
            lastListSize = size;
            return new PagedPhysicalAssets(assetsToReturn, totalCountToReturn);
        }

        @Override
        public PhysicalAsset updateAsset(UUID id, String assetName, String make, String model,
                                          Map<String, String> metadata, Boolean isActive) {
            lastUpdateAssetId = id;
            lastUpdateMetadata = metadata;
            return assetToReturn;
        }

        @Override
        public void deactivateAsset(UUID id) {
            lastDeactivateAssetId = id;
        }
    }
}
