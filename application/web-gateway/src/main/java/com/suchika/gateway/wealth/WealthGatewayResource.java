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
    public JsonNode getAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId) {
        return wealthServiceClient.getAccount(accountId, profileId);
    }

    @PATCH
    @Path("/accounts/{accountId}")
    public JsonNode updateAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        return wealthServiceClient.updateAccount(accountId, profileId, body);
    }

    @GET
    @Path("/accounts/{accountId}/balance")
    public JsonNode getAccountBalance(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId) {
        return wealthServiceClient.getAccountBalance(accountId, profileId);
    }

    @PATCH
    @Path("/accounts/{accountId}/classification")
    public JsonNode updateAccountClassification(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        return wealthServiceClient.updateAccountClassification(accountId, profileId, body);
    }

    @DELETE
    @Path("/accounts/{accountId}")
    public Response deactivateAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId) {
        wealthServiceClient.deactivateAccount(accountId, profileId);
        return Response.noContent().build();
    }

    @POST
    @Path("/accounts/{accountId}/transactions")
    public Response createTransaction(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        AppLogger.info("Gateway: creating manual transaction for account %s", accountId);
        try (Response upstream = wealthServiceClient.createTransaction(accountId, profileId, body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/accounts/{accountId}/transactions")
    public JsonNode listTransactions(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("txn_type") String txnType,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return wealthServiceClient.listTransactions(accountId, profileId, from, to, txnType, page, size);
    }

    @GET
    @Path("/accounts/{accountId}/transactions/{txnId}")
    public JsonNode getTransaction(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId) {
        return wealthServiceClient.getTransaction(accountId, txnId);
    }

    @PATCH
    @Path("/accounts/{accountId}/transactions/{txnId}/category")
    public JsonNode updateTransactionCategory(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId,
            JsonNode body) {
        return wealthServiceClient.updateTransactionCategory(accountId, txnId, body);
    }

    @PATCH
    @Path("/accounts/{accountId}/transactions/category")
    public JsonNode bulkUpdateTransactionCategory(@PathParam("accountId") UUID accountId, JsonNode body) {
        return wealthServiceClient.bulkUpdateTransactionCategory(accountId, body);
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

    @GET
    @Path("/accounts/{accountId}/uploads/{uploadId}/errors")
    public JsonNode getUploadErrors(
            @PathParam("accountId") UUID accountId,
            @PathParam("uploadId") UUID uploadId) {
        return wealthServiceClient.getUploadErrors(accountId, uploadId);
    }

    @GET
    @Path("/physical-assets")
    public JsonNode listPhysicalAssets(
            @QueryParam("asset_type") String assetType,
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") String profileId,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return wealthServiceClient.listPhysicalAssets(assetType, isActive, profileId, page, size);
    }

    @POST
    @Path("/physical-assets")
    public Response createPhysicalAsset(
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        AppLogger.info("Gateway: creating physical asset");
        try (Response upstream = wealthServiceClient.createPhysicalAsset(profileId, body)) {
            return Response.status(upstream.getStatus())
                    .entity(upstream.readEntity(String.class))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/physical-assets/{assetId}")
    public JsonNode getPhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId) {
        return wealthServiceClient.getPhysicalAsset(assetId, profileId);
    }

    @PATCH
    @Path("/physical-assets/{assetId}")
    public JsonNode updatePhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId,
            JsonNode body) {
        return wealthServiceClient.updatePhysicalAsset(assetId, profileId, body);
    }

    @DELETE
    @Path("/physical-assets/{assetId}")
    public Response deactivatePhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId) {
        wealthServiceClient.deactivatePhysicalAsset(assetId, profileId);
        return Response.noContent().build();
    }

    @GET
    @Path("/accounts/{accountId}/amortization")
    public JsonNode getAmortization(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId) {
        return wealthServiceClient.getAmortization(accountId, profileId);
    }
}
