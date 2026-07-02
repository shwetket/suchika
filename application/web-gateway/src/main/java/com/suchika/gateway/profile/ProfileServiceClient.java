package com.suchika.gateway.profile;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "profile-service")
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ProfileServiceClient {

    @GET
    @Path("/profiles")
    JsonNode listProfiles(
            @QueryParam("admin_id") UUID adminId,
            @QueryParam("is_active") Boolean isActive);

    @POST
    @Path("/profiles")
    Response createProfile(JsonNode body);

    @GET
    @Path("/profiles/{profileId}")
    JsonNode getProfile(@PathParam("profileId") UUID profileId);

    @PATCH
    @Path("/profiles/{profileId}")
    JsonNode updateProfile(@PathParam("profileId") UUID profileId, JsonNode body);

    @DELETE
    @Path("/profiles/{profileId}")
    void deactivateProfile(@PathParam("profileId") UUID profileId);

    @GET
    @Path("/admins")
    JsonNode listAdmins();

    @GET
    @Path("/admins/{adminId}")
    JsonNode getAdmin(@PathParam("adminId") UUID adminId);
}
