package com.suchika.gateway.health;

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
public class HealthGatewayResource {

    private final HealthServiceClient healthServiceClient;

    @Inject
    public HealthGatewayResource(@RestClient HealthServiceClient healthServiceClient) {
        this.healthServiceClient = healthServiceClient;
    }

    @GET
    @Path("/vitals")
    public JsonNode listVitals(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("vital_type") String vitalType) {
        return healthServiceClient.listVitals(profileId, vitalType);
    }

    @POST
    @Path("/vitals")
    public Response recordVital(JsonNode body) {
        AppLogger.info("Gateway: recording vital reading");
        try (Response upstream = healthServiceClient.recordVital(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/vitals/{id}")
    public JsonNode getVital(@PathParam("id") UUID id) {
        return healthServiceClient.getVital(id);
    }

    @PATCH
    @Path("/vitals/{id}")
    public JsonNode updateVital(@PathParam("id") UUID id, JsonNode body) {
        return healthServiceClient.updateVital(id, body);
    }

    @DELETE
    @Path("/vitals/{id}")
    public Response deleteVital(@PathParam("id") UUID id) {
        healthServiceClient.deleteVital(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/doctor-visits")
    public JsonNode listDoctorVisits(@QueryParam("profile_id") UUID profileId) {
        return healthServiceClient.listDoctorVisits(profileId);
    }

    @POST
    @Path("/doctor-visits")
    public Response createDoctorVisit(JsonNode body) {
        AppLogger.info("Gateway: creating doctor visit");
        try (Response upstream = healthServiceClient.createDoctorVisit(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/doctor-visits/{id}")
    public JsonNode getDoctorVisit(@PathParam("id") UUID id) {
        return healthServiceClient.getDoctorVisit(id);
    }

    @PATCH
    @Path("/doctor-visits/{id}")
    public JsonNode updateDoctorVisit(@PathParam("id") UUID id, JsonNode body) {
        return healthServiceClient.updateDoctorVisit(id, body);
    }

    @DELETE
    @Path("/doctor-visits/{id}")
    public Response deleteDoctorVisit(@PathParam("id") UUID id) {
        healthServiceClient.deleteDoctorVisit(id);
        return Response.noContent().build();
    }
}
