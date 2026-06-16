package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.ListVitalReadingsResponse;
import com.suchika.health.adapters.http.dto.RecordVitalReadingRequest;
import com.suchika.health.adapters.http.dto.VitalReadingResponse;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.input.VitalReadingUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/vitals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VitalReadingResource {

    private final VitalReadingUseCase useCase;

    public VitalReadingResource(VitalReadingUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listVitals(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("vital_type") String vitalTypeParam) {

        VitalType vitalType = parseVitalType(vitalTypeParam);
        List<VitalReadingResponse> readings = useCase.listByProfile(profileId, vitalType)
                .stream().map(VitalReadingResponse::from).toList();
        return Response.ok(new ListVitalReadingsResponse(readings)).build();
    }

    @POST
    public Response recordVitalReading(RecordVitalReadingRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        VitalType vitalType = parseVitalType(request.vitalType);
        VitalReadingResponse response = VitalReadingResponse.from(
                useCase.recordReading(request.profileId, vitalType, request.readingDate,
                        request.valuePrimary, request.valueSecondary, request.unit, request.notes));
        return Response.status(201).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public VitalReadingResponse getById(@PathParam("id") UUID id) {
        return VitalReadingResponse.from(useCase.getById(id));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

    private VitalType parseVitalType(String value) {
        if (value == null) return null;
        try {
            return VitalType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vital_type: " + value);
        }
    }
}
