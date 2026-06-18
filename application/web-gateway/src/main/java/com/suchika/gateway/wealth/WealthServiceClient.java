package com.suchika.gateway.wealth;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

@RegisterRestClient(configKey = "wealth-service")
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WealthServiceClient {

    @GET
    @Path("/accounts")
    JsonNode listAccounts(
            @QueryParam("account_type") String accountType,
            @QueryParam("is_active") Boolean isActive);

    @POST
    @Path("/accounts")
    Response createAccount(JsonNode body);

    @GET
    @Path("/accounts/{accountId}")
    JsonNode getAccount(@PathParam("accountId") UUID accountId);

    @PATCH
    @Path("/accounts/{accountId}")
    JsonNode updateAccount(@PathParam("accountId") UUID accountId, JsonNode body);

    @DELETE
    @Path("/accounts/{accountId}")
    void deactivateAccount(@PathParam("accountId") UUID accountId);

    @GET
    @Path("/accounts/{accountId}/transactions")
    JsonNode listTransactions(
            @PathParam("accountId") UUID accountId,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("txn_type") String txnType);

    @GET
    @Path("/accounts/{accountId}/transactions/{txnId}")
    JsonNode getTransaction(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId);

    @POST
    @Path("/accounts/{accountId}/uploads")
    Response uploadStatement(@PathParam("accountId") UUID accountId, JsonNode body);

    @GET
    @Path("/accounts/{accountId}/uploads")
    JsonNode listUploads(@PathParam("accountId") UUID accountId);

    @DELETE
    @Path("/accounts/{accountId}/uploads/{uploadId}")
    void rollbackUpload(
            @PathParam("accountId") UUID accountId,
            @PathParam("uploadId") UUID uploadId);
}
