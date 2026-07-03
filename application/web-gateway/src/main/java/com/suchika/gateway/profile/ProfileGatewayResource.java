package com.suchika.gateway.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.suchika.shared.logging.AppLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileGatewayResource {

    private final ProfileServiceClient profileServiceClient;

    @Inject
    public ProfileGatewayResource(@RestClient ProfileServiceClient profileServiceClient) {
        this.profileServiceClient = profileServiceClient;
    }

    @GET
    @Path("/admins")
    public JsonNode listAdmins() {
        return profileServiceClient.listAdmins();
    }

    @POST
    @Path("/admins")
    public Response createAdmin(JsonNode body) {
        AppLogger.info("Gateway: creating admin");
        try (Response upstream = profileServiceClient.createAdmin(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @PATCH
    @Path("/admins/{adminId}/policy")
    public JsonNode updatePolicySettings(@PathParam("adminId") UUID adminId, JsonNode body) {
        return profileServiceClient.updatePolicySettings(adminId, body);
    }

    @GET
    @Path("/profiles")
    public JsonNode listProfiles(
            @QueryParam("admin_id") UUID adminId,
            @QueryParam("is_active") Boolean isActive) {
        return profileServiceClient.listProfiles(adminId, isActive);
    }

    @POST
    @Path("/profiles")
    public Response createProfile(JsonNode body) {
        AppLogger.info("Gateway: creating profile");
        try (Response upstream = profileServiceClient.createProfile(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/profiles/{profileId}")
    public JsonNode getProfile(@PathParam("profileId") UUID profileId) {
        return profileServiceClient.getProfile(profileId);
    }

    @PATCH
    @Path("/profiles/{profileId}")
    public JsonNode updateProfile(@PathParam("profileId") UUID profileId, JsonNode body) {
        return profileServiceClient.updateProfile(profileId, body);
    }

    @DELETE
    @Path("/profiles/{profileId}")
    public Response deactivateProfile(@PathParam("profileId") UUID profileId) {
        profileServiceClient.deactivateProfile(profileId);
        return Response.noContent().build();
    }
}
