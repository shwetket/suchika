package com.suchika.household.adapters.http;

import com.suchika.household.adapters.http.dto.CreateInventoryItemRequest;
import com.suchika.household.adapters.http.dto.InventoryItemDto;
import com.suchika.household.adapters.http.dto.ListInventoryItemsResponse;
import com.suchika.household.adapters.http.dto.UpdateInventoryItemRequest;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.input.InventoryItemUseCase;
import com.suchika.household.ports.input.PagedInventoryItems;
import com.suchika.household.ports.input.UpdateInventoryItemCommand;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

import java.util.List;
import java.util.UUID;

@Path("/v1/inventory-items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryItemResource {

    private final InventoryItemUseCase useCase;

    public InventoryItemResource(InventoryItemUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public ListInventoryItemsResponse list(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("source_platform") String sourcePlatform,
            @QueryParam("category") String category,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {
        profileId = ResourceUtils.requireProfileId(profileId);
        SourcePlatform platform = parseSourcePlatform(sourcePlatform);
        int page = ResourceUtils.parsePage(pageParam);
        int size = ResourceUtils.parseSize(sizeParam);

        PagedInventoryItems result = useCase.listPaginated(profileId, platform, category, page, size);
        List<InventoryItemDto> items = result.items().stream().map(InventoryItemDto::from).toList();
        return new ListInventoryItemsResponse(items, result.totalCount(), page, size);
    }

    @POST
    public Response create(CreateInventoryItemRequest request) {
        if (request.profileId == null) throw new BadRequestException("profile_id is required");
        if (request.itemName == null || request.itemName.isBlank()) throw new BadRequestException("item_name is required");
        if (request.quantity == null) throw new BadRequestException("quantity is required");
        if (request.unit == null) throw new BadRequestException("unit is required");
        if (request.sourcePlatform == null) throw new BadRequestException("source_platform is required");
        if (request.purchaseDate == null) throw new BadRequestException("purchase_date is required");

        ItemUnit unit = parseItemUnit(request.unit);
        SourcePlatform platform = parseSourcePlatform(request.sourcePlatform);

        InventoryItemDto created = InventoryItemDto.from(useCase.create(
                request.profileId, request.itemName, request.quantity,
                unit, platform, request.purchaseDate, request.category));
        return Response.status(201).entity(created).build();
    }

    @GET
    @Path("/{id}")
    public InventoryItemDto get(@PathParam("id") UUID id) {
        return InventoryItemDto.from(useCase.get(id));
    }

    @PUT
    @Path("/{id}")
    public InventoryItemDto update(@PathParam("id") UUID id, UpdateInventoryItemRequest request) {
        ItemUnit unit = parseItemUnit(request.unit);
        SourcePlatform platform = parseSourcePlatform(request.sourcePlatform);
        UpdateInventoryItemCommand command = new UpdateInventoryItemCommand(request.itemName, request.quantity,
                unit, platform, request.purchaseDate, request.category, request.isConsumed);
        return InventoryItemDto.from(useCase.update(id, command));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

    private SourcePlatform parseSourcePlatform(String value) {
        if (value == null) return null;
        try {
            return SourcePlatform.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid source_platform: " + value);
        }
    }

    private ItemUnit parseItemUnit(String value) {
        if (value == null) return null;
        try {
            return ItemUnit.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid unit: " + value);
        }
    }


}
