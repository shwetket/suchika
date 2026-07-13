package com.suchika.gateway.health;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "health-service")
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface HealthServiceClient {

    @GET
    @Path("/vitals")
    JsonNode listVitals(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("vital_type") String vitalType,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/vitals")
    Response recordVital(JsonNode body);

    @GET
    @Path("/vitals/{id}")
    JsonNode getVital(@PathParam("id") UUID id);

    @PATCH
    @Path("/vitals/{id}")
    JsonNode updateVital(@PathParam("id") UUID id, JsonNode body);

    @DELETE
    @Path("/vitals/{id}")
    void deleteVital(@PathParam("id") UUID id);

    @GET
    @Path("/doctor-visits")
    JsonNode listDoctorVisits(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/doctor-visits")
    Response createDoctorVisit(JsonNode body);

    @GET
    @Path("/doctor-visits/{id}")
    JsonNode getDoctorVisit(@PathParam("id") UUID id);

    @PATCH
    @Path("/doctor-visits/{id}")
    JsonNode updateDoctorVisit(@PathParam("id") UUID id, JsonNode body);

    @DELETE
    @Path("/doctor-visits/{id}")
    void deleteDoctorVisit(@PathParam("id") UUID id);

    // ── Errors (Phase 4 Application Console, ADR-023) ───────────────────────

    @GET
    @Path("/errors")
    JsonNode listErrors(@QueryParam("since") String since, @QueryParam("limit") Integer limit);
}
