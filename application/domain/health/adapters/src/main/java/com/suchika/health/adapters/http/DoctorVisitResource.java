package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.CreateDoctorVisitRequest;
import com.suchika.health.adapters.http.dto.DoctorVisitResponse;
import com.suchika.health.adapters.http.dto.ListDoctorVisitsResponse;
import com.suchika.health.adapters.http.dto.UpdateDoctorVisitRequest;
import com.suchika.health.ports.input.CreateDoctorVisitCommand;
import com.suchika.health.ports.input.DoctorVisitUseCase;
import com.suchika.health.ports.input.UpdateDoctorVisitCommand;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/doctor-visits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DoctorVisitResource {

    private final DoctorVisitUseCase useCase;

    public DoctorVisitResource(DoctorVisitUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listVisits(@QueryParam("profile_id") UUID profileId) {
        List<DoctorVisitResponse> visits = useCase.listByProfile(profileId)
                .stream().map(DoctorVisitResponse::from).toList();
        return Response.ok(new ListDoctorVisitsResponse(visits)).build();
    }

    @POST
    public Response create(CreateDoctorVisitRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        CreateDoctorVisitCommand command = new CreateDoctorVisitCommand(
                request.profileId, request.fromDate, request.toDate,
                request.visitedDoctor != null && request.visitedDoctor,
                request.doctorName, request.hospitalName, request.speciality,
                request.symptoms, request.diagnosis, request.notes, request.followUpDate);
        DoctorVisitResponse response = DoctorVisitResponse.from(useCase.create(command));
        return Response.status(201).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public DoctorVisitResponse getById(@PathParam("id") UUID id) {
        return DoctorVisitResponse.from(useCase.getById(id));
    }

    @PATCH
    @Path("/{id}")
    public DoctorVisitResponse update(@PathParam("id") UUID id, UpdateDoctorVisitRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        UpdateDoctorVisitCommand command = new UpdateDoctorVisitCommand(
                request.toDate, request.doctorName, request.hospitalName, request.speciality,
                request.symptoms, request.diagnosis, request.notes, request.followUpDate);
        return DoctorVisitResponse.from(useCase.update(id, command));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

}
