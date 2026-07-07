package com.suchika.gateway.household;

import com.fasterxml.jackson.databind.JsonNode;
import com.suchika.shared.logging.AppLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

@Path("/v1/household")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HouseholdGatewayResource {

    private final HouseholdServiceClient householdServiceClient;

    @Inject
    public HouseholdGatewayResource(@RestClient HouseholdServiceClient householdServiceClient) {
        this.householdServiceClient = householdServiceClient;
    }

    // ── Calendar Events ──────────────────────────────────────────────────────

    @GET
    @Path("/calendar-events")
    public JsonNode listCalendarEvents(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("event_type") String eventType,
            @QueryParam("from_date") String fromDate,
            @QueryParam("to_date") String toDate,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return householdServiceClient.listCalendarEvents(profileId, eventType, fromDate, toDate, page, size);
    }

    @POST
    @Path("/calendar-events")
    public Response createCalendarEvent(JsonNode body) {
        AppLogger.info("Gateway: creating calendar event");
        try (Response upstream = householdServiceClient.createCalendarEvent(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/calendar-events/{id}")
    public JsonNode getCalendarEvent(@PathParam("id") UUID id) {
        return householdServiceClient.getCalendarEvent(id);
    }

    @PATCH
    @Path("/calendar-events/{id}")
    public JsonNode updateCalendarEvent(@PathParam("id") UUID id, JsonNode body) {
        return householdServiceClient.updateCalendarEvent(id, body);
    }

    @DELETE
    @Path("/calendar-events/{id}")
    public Response deleteCalendarEvent(@PathParam("id") UUID id) {
        householdServiceClient.deleteCalendarEvent(id);
        return Response.noContent().build();
    }

    // ── Inventory Items ──────────────────────────────────────────────────────

    @GET
    @Path("/inventory-items")
    public JsonNode listInventoryItems(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("source_platform") String sourcePlatform,
            @QueryParam("category") String category,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return householdServiceClient.listInventoryItems(profileId, sourcePlatform, category, page, size);
    }

    @POST
    @Path("/inventory-items")
    public Response createInventoryItem(JsonNode body) {
        AppLogger.info("Gateway: creating inventory item");
        try (Response upstream = householdServiceClient.createInventoryItem(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/inventory-items/{id}")
    public JsonNode getInventoryItem(@PathParam("id") UUID id) {
        return householdServiceClient.getInventoryItem(id);
    }

    @PUT
    @Path("/inventory-items/{id}")
    public JsonNode updateInventoryItem(@PathParam("id") UUID id, JsonNode body) {
        return householdServiceClient.updateInventoryItem(id, body);
    }

    @DELETE
    @Path("/inventory-items/{id}")
    public Response deleteInventoryItem(@PathParam("id") UUID id) {
        householdServiceClient.deleteInventoryItem(id);
        return Response.noContent().build();
    }

    // ── Goals ────────────────────────────────────────────────────────────────

    @GET
    @Path("/goals")
    public JsonNode listGoals(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("status") String status,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return householdServiceClient.listGoals(profileId, status, page, size);
    }

    @POST
    @Path("/goals")
    public Response createGoal(JsonNode body) {
        AppLogger.info("Gateway: creating goal");
        try (Response upstream = householdServiceClient.createGoal(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/goals/{id}")
    public JsonNode getGoal(@PathParam("id") UUID id) {
        return householdServiceClient.getGoal(id);
    }

    @PATCH
    @Path("/goals/{id}")
    public JsonNode updateGoal(@PathParam("id") UUID id, JsonNode body) {
        return householdServiceClient.updateGoal(id, body);
    }

    @DELETE
    @Path("/goals/{id}")
    public Response deleteGoal(@PathParam("id") UUID id) {
        householdServiceClient.deleteGoal(id);
        return Response.noContent().build();
    }
}
