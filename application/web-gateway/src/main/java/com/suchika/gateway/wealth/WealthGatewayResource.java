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
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") String profileId) {
        return wealthServiceClient.listAccounts(accountType, isActive, profileId);
    }

    @POST
    @Path("/accounts")
    public Response createAccount(
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        AppLogger.info("Gateway: creating account");
        try (Response upstream = wealthServiceClient.createAccount(profileId, body)) {
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

    @GET
    @Path("/accounts/{accountId}/transactions")
    public JsonNode listTransactions(
            @PathParam("accountId") UUID accountId,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("txn_type") String txnType) {
        return wealthServiceClient.listTransactions(accountId, from, to, txnType);
    }

    @GET
    @Path("/accounts/{accountId}/transactions/{txnId}")
    public JsonNode getTransaction(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId) {
        return wealthServiceClient.getTransaction(accountId, txnId);
    }

    @POST
    @Path("/accounts/{accountId}/uploads")
    public Response uploadStatement(@PathParam("accountId") UUID accountId, JsonNode body) {
        AppLogger.info("Gateway: uploading statement for account %s", accountId);
        try (Response upstream = wealthServiceClient.uploadStatement(accountId, body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/accounts/{accountId}/uploads")
    public JsonNode listUploads(@PathParam("accountId") UUID accountId) {
        return wealthServiceClient.listUploads(accountId);
    }

    @DELETE
    @Path("/accounts/{accountId}/uploads/{uploadId}")
    public Response rollbackUpload(
            @PathParam("accountId") UUID accountId,
            @PathParam("uploadId") UUID uploadId) {
        wealthServiceClient.rollbackUpload(accountId, uploadId);
        return Response.noContent().build();
    }
}
