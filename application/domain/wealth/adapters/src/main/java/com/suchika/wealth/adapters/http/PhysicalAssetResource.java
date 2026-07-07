package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.CreatePhysicalAssetRequest;
import com.suchika.wealth.adapters.http.dto.ListPhysicalAssetsResponse;
import com.suchika.wealth.adapters.http.dto.PhysicalAssetResponse;
import com.suchika.wealth.adapters.http.dto.UpdatePhysicalAssetRequest;
import com.suchika.wealth.domain.AssetType;
import com.suchika.wealth.domain.RegistrationType;
import com.suchika.wealth.ports.input.CreatePhysicalAssetCommand;
import com.suchika.wealth.ports.input.PagedPhysicalAssets;
import com.suchika.wealth.ports.input.PhysicalAssetUseCase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

import java.util.List;
import java.util.UUID;

@Path("/v1/physical-assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhysicalAssetResource {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final PhysicalAssetUseCase useCase;

    public PhysicalAssetResource(PhysicalAssetUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listAssets(
            @QueryParam("asset_type") String assetTypeParam,
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {
        AssetType assetType = parseAssetType(assetTypeParam);
        int page = parsePage(pageParam);
        int size = parseSize(sizeParam);

        PagedPhysicalAssets result = useCase.listAssetsPaginated(profileId, assetType, isActive, page, size);
        List<PhysicalAssetResponse> assets = result.assets().stream().map(PhysicalAssetResponse::from).toList();
        return Response.ok(new ListPhysicalAssetsResponse(assets, result.totalCount(), page, size)).build();
    }

    @POST
    public Response createAsset(
            @QueryParam("profile_id") UUID profileId,
            CreatePhysicalAssetRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        AssetType assetType = parseAssetType(request.assetType);
        if (assetType == null) throw new BadRequestException("asset_type is required");
        RegistrationType registrationType = parseRegistrationType(request.registrationType);
        if (registrationType == null) throw new BadRequestException("registration_type is required");
        CreatePhysicalAssetCommand command = new CreatePhysicalAssetCommand(
                request.assetName, assetType, request.make, request.model,
                request.registrationNumber, registrationType);
        return Response.status(201)
                .entity(PhysicalAssetResponse.from(useCase.createAsset(profileId, command)))
                .build();
    }

    @GET
    @Path("/{asset_id}")
    public PhysicalAssetResponse getAsset(@PathParam("asset_id") UUID assetId) {
        return PhysicalAssetResponse.from(useCase.getAsset(assetId));
    }

    @PATCH
    @Path("/{asset_id}")
    public PhysicalAssetResponse updateAsset(@PathParam("asset_id") UUID assetId, UpdatePhysicalAssetRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        return PhysicalAssetResponse.from(
                useCase.updateAsset(assetId, request.assetName, request.make, request.model,
                        request.metadata, request.active));
    }

    @DELETE
    @Path("/{asset_id}")
    public Response deactivateAsset(@PathParam("asset_id") UUID assetId) {
        useCase.deactivateAsset(assetId);
        return Response.noContent().build();
    }

    private AssetType parseAssetType(String value) {
        if (value == null) return null;
        try {
            return AssetType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid asset_type: " + value);
        }
    }

    private RegistrationType parseRegistrationType(String value) {
        if (value == null) return null;
        try {
            return RegistrationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid registration_type: " + value);
        }
    }

    private int parsePage(Integer pageParam) {
        int page = pageParam != null ? pageParam : 0;
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        return page;
    }

    private int parseSize(Integer sizeParam) {
        int size = sizeParam != null ? sizeParam : DEFAULT_PAGE_SIZE;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
