package com.suchika.gateway.wealth;

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
public class WealthGatewayResource {

    private final WealthServiceClient wealthServiceClient;

    @Inject
    public WealthGatewayResource(@RestClient WealthServiceClient wealthServiceClient) {
        this.wealthServiceClient = wealthServiceClient;
    }

    @GET
    @Path("/accounts")
    public JsonNode listAccounts(
            @QueryParam("account_type") String accountType,
            @QueryParam("is_active") Boolean isActive) {
        return wealthServiceClient.listAccounts(accountType, isActive);
    }

    @POST
    @Path("/accounts")
    public Response createAccount(JsonNode body) {
        AppLogger.info("Gateway: creating account");
        try (Response upstream = wealthServiceClient.createAccount(body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/accounts/{accountId}")
    public JsonNode getAccount(@PathParam("accountId") UUID accountId) {
        return wealthServiceClient.getAccount(accountId);
    }

    @PATCH
    @Path("/accounts/{accountId}")
    public JsonNode updateAccount(@PathParam("accountId") UUID accountId, JsonNode body) {
        return wealthServiceClient.updateAccount(accountId, body);
    }

    @DELETE
    @Path("/accounts/{accountId}")
    public Response deactivateAccount(@PathParam("accountId") UUID accountId) {
        wealthServiceClient.deactivateAccount(accountId);
        return Response.noContent().build();
    }
}
