package com.suchika.gateway.household;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "household-service")
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface HouseholdServiceClient {

    // ── Calendar Events ──────────────────────────────────────────────────────

    @GET
    @Path("/calendar-events")
    JsonNode listCalendarEvents(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("event_type") String eventType,
            @QueryParam("from_date") String fromDate,
            @QueryParam("to_date") String toDate,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/calendar-events")
    Response createCalendarEvent(JsonNode body);

    @GET
    @Path("/calendar-events/{id}")
    JsonNode getCalendarEvent(@PathParam("id") UUID id);

    @PATCH
    @Path("/calendar-events/{id}")
    JsonNode updateCalendarEvent(@PathParam("id") UUID id, JsonNode body);

    @DELETE
    @Path("/calendar-events/{id}")
    void deleteCalendarEvent(@PathParam("id") UUID id);

    // ── Inventory Items ──────────────────────────────────────────────────────

    @GET
    @Path("/inventory-items")
    JsonNode listInventoryItems(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("source_platform") String sourcePlatform,
            @QueryParam("category") String category,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/inventory-items")
    Response createInventoryItem(JsonNode body);

    @GET
    @Path("/inventory-items/{id}")
    JsonNode getInventoryItem(@PathParam("id") UUID id);

    @PUT
    @Path("/inventory-items/{id}")
    JsonNode updateInventoryItem(@PathParam("id") UUID id, JsonNode body);

    @DELETE
    @Path("/inventory-items/{id}")
    void deleteInventoryItem(@PathParam("id") UUID id);

    // ── Goals ────────────────────────────────────────────────────────────────

    @GET
    @Path("/goals")
    JsonNode listGoals(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("status") String status,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/goals")
    Response createGoal(JsonNode body);

    @GET
    @Path("/goals/{id}")
    JsonNode getGoal(@PathParam("id") UUID id);

    @PATCH
    @Path("/goals/{id}")
    JsonNode updateGoal(@PathParam("id") UUID id, JsonNode body);

    @DELETE
    @Path("/goals/{id}")
    void deleteGoal(@PathParam("id") UUID id);

    @PUT
    @Path("/goals/{id}/current-amount")
    JsonNode updateGoalCurrentAmount(@PathParam("id") UUID id, JsonNode body);

    // ── Errors (Phase 4 Application Console, ADR-023) ───────────────────────

    @GET
    @Path("/errors")
    JsonNode listErrors(@QueryParam("since") String since, @QueryParam("limit") Integer limit);
}
