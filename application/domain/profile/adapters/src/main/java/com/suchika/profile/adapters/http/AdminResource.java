package com.suchika.profile.adapters.http;

import com.suchika.profile.adapters.http.dto.*;
import com.suchika.profile.domain.Admin;
import com.suchika.profile.ports.input.AdminUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/admins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    AdminUseCase adminUseCase;

    @GET
    public ListAdminsResponse listAdmins() {
        List<AdminResponse> admins = adminUseCase.listAdmins()
            .stream().map(AdminResponse::from).toList();
        return new ListAdminsResponse(admins);
    }

    @POST
    public Response createAdmin(CreateAdminRequest request) {
        if (request.displayName == null || request.displayName.isBlank()) {
            throw new BadRequestException("display_name is required");
        }
        Admin admin = adminUseCase.createAdmin(request.displayName, request.emailAddress);
        return Response.status(201).entity(AdminResponse.from(admin)).build();
    }

    @GET
    @Path("/{admin_id}")
    public AdminResponse getAdmin(@PathParam("admin_id") UUID adminId) {
        return AdminResponse.from(adminUseCase.getAdmin(adminId));
    }

    @PATCH
    @Path("/{admin_id}")
    public AdminResponse updateAdmin(@PathParam("admin_id") UUID adminId, UpdateAdminRequest request) {
        Admin admin = adminUseCase.updateAdmin(adminId, request.displayName, request.emailAddress, request.isActive);
        return AdminResponse.from(admin);
    }

    @DELETE
    @Path("/{admin_id}")
    public Response deactivateAdmin(@PathParam("admin_id") UUID adminId) {
        adminUseCase.deactivateAdmin(adminId);
        return Response.noContent().build();
    }
}
