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
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") String profileId);

    @POST
    @Path("/accounts")
    Response createAccount(
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @GET
    @Path("/accounts/{accountId}")
    JsonNode getAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId);

    @GET
    @Path("/accounts/{accountId}/balance")
    JsonNode getAccountBalance(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId);

    @PATCH
    @Path("/accounts/{accountId}")
    JsonNode updateAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @PATCH
    @Path("/accounts/{accountId}/classification")
    JsonNode updateAccountClassification(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @DELETE
    @Path("/accounts/{accountId}")
    void deactivateAccount(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId);

    @POST
    @Path("/accounts/{accountId}/transactions")
    Response createTransaction(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @GET
    @Path("/accounts/{accountId}/transactions")
    JsonNode listTransactions(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("txn_type") String txnType,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @GET
    @Path("/accounts/{accountId}/transactions/{txnId}")
    JsonNode getTransaction(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId);

    @PATCH
    @Path("/accounts/{accountId}/transactions/{txnId}/category")
    JsonNode updateTransactionCategory(
            @PathParam("accountId") UUID accountId,
            @PathParam("txnId") UUID txnId,
            JsonNode body);

    @PATCH
    @Path("/accounts/{accountId}/transactions/category")
    JsonNode bulkUpdateTransactionCategory(@PathParam("accountId") UUID accountId, JsonNode body);

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

    @GET
    @Path("/accounts/{accountId}/uploads/{uploadId}/errors")
    JsonNode getUploadErrors(
            @PathParam("accountId") UUID accountId,
            @PathParam("uploadId") UUID uploadId);

    @GET
    @Path("/physical-assets")
    JsonNode listPhysicalAssets(
            @QueryParam("asset_type") String assetType,
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") String profileId,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size);

    @POST
    @Path("/physical-assets")
    Response createPhysicalAsset(
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @GET
    @Path("/physical-assets/{assetId}")
    JsonNode getPhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId);

    @PATCH
    @Path("/physical-assets/{assetId}")
    JsonNode updatePhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId,
            JsonNode body);

    @DELETE
    @Path("/physical-assets/{assetId}")
    void deactivatePhysicalAsset(
            @PathParam("assetId") UUID assetId,
            @QueryParam("profile_id") String profileId);

    @GET
    @Path("/accounts/{accountId}/amortization")
    JsonNode getAmortization(
            @PathParam("accountId") UUID accountId,
            @QueryParam("profile_id") String profileId);

    // ── Goal Plans (ADR-022 Phase 1) — pure JsonNode passthrough ──────────────

    @GET
    @Path("/goal-plans")
    JsonNode listGoalPlans(@QueryParam("admin_id") UUID adminId);

    @POST
    @Path("/goal-plans")
    Response createGoalPlan(@QueryParam("admin_id") UUID adminId, JsonNode body);

    @GET
    @Path("/goal-plans/{goalPlanId}")
    JsonNode getGoalPlan(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId);

    @PATCH
    @Path("/goal-plans/{goalPlanId}")
    JsonNode updateGoalPlan(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    @DELETE
    @Path("/goal-plans/{goalPlanId}")
    void deactivateGoalPlan(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId);

    @PUT
    @Path("/goal-plans/{goalPlanId}/milestones")
    JsonNode replaceGoalPlanMilestones(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    @PATCH
    @Path("/goal-plans/{goalPlanId}/milestones/{milestoneId}")
    JsonNode updateGoalPlanMilestoneAchieved(
            @PathParam("goalPlanId") UUID goalPlanId,
            @PathParam("milestoneId") UUID milestoneId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    @PUT
    @Path("/goal-plans/{goalPlanId}/rules")
    JsonNode replaceGoalPlanRules(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    @PUT
    @Path("/goal-plans/{goalPlanId}/trigger-events")
    JsonNode replaceGoalPlanTriggerEvents(
            @PathParam("goalPlanId") UUID goalPlanId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    // ── Insurance Policies (ADR-022 Phase 2) — pure JsonNode passthrough ──────

    @GET
    @Path("/insurance-policies")
    JsonNode listInsurancePolicies(@QueryParam("admin_id") UUID adminId);

    @POST
    @Path("/insurance-policies")
    Response createInsurancePolicy(@QueryParam("admin_id") UUID adminId, JsonNode body);

    @GET
    @Path("/insurance-policies/{insurancePolicyId}")
    JsonNode getInsurancePolicy(
            @PathParam("insurancePolicyId") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId);

    @PATCH
    @Path("/insurance-policies/{insurancePolicyId}")
    JsonNode updateInsurancePolicy(
            @PathParam("insurancePolicyId") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId,
            JsonNode body);

    @DELETE
    @Path("/insurance-policies/{insurancePolicyId}")
    void deactivateInsurancePolicy(
            @PathParam("insurancePolicyId") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId);
}
