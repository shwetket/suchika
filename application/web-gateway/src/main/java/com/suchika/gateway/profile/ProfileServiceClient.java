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

    @POST
    @Path("/admins")
    Response createAdmin(JsonNode body);

    @GET
    @Path("/admins/{adminId}")
    JsonNode getAdmin(@PathParam("adminId") UUID adminId);

    @PATCH
    @Path("/admins/{adminId}/policy")
    JsonNode updatePolicySettings(@PathParam("adminId") UUID adminId, JsonNode body);

    // ── Errors (Phase 4 Application Console, ADR-023) ───────────────────────

    @GET
    @Path("/errors")
    JsonNode listErrors(@QueryParam("since") String since, @QueryParam("limit") Integer limit);
}
