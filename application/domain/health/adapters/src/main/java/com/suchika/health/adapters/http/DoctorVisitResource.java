package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.CreateDoctorVisitRequest;
import com.suchika.health.adapters.http.dto.DoctorVisitResponse;
import com.suchika.health.adapters.http.dto.ListDoctorVisitsResponse;
import com.suchika.health.adapters.http.dto.UpdateDoctorVisitRequest;
import com.suchika.health.ports.input.CreateDoctorVisitCommand;
import com.suchika.health.ports.input.DoctorVisitUseCase;
import com.suchika.health.ports.input.PagedDoctorVisits;
import com.suchika.health.ports.input.UpdateDoctorVisitCommand;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/v1/doctor-visits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DoctorVisitResource {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final DoctorVisitUseCase useCase;

    public DoctorVisitResource(DoctorVisitUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listVisits(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("from") String fromParam,
            @QueryParam("to") String toParam,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {
        LocalDate from = parseDate(fromParam, "from");
        LocalDate to = parseDate(toParam, "to");
        int page = parsePage(pageParam);
        int size = parseSize(sizeParam);

        PagedDoctorVisits result = useCase.listByProfilePaginated(profileId, from, to, page, size);
        List<DoctorVisitResponse> visits = result.visits().stream().map(DoctorVisitResponse::from).toList();
        return Response.ok(new ListDoctorVisitsResponse(visits, result.totalCount(), page, size)).build();
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
