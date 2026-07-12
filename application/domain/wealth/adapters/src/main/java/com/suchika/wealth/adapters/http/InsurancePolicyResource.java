package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.utils.ResourceUtils;
import com.suchika.wealth.adapters.http.dto.CreateInsurancePolicyRequest;
import com.suchika.wealth.adapters.http.dto.InsurancePolicyResponse;
import com.suchika.wealth.adapters.http.dto.ListInsurancePoliciesResponse;
import com.suchika.wealth.adapters.http.dto.UpdateInsurancePolicyRequest;
import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;
import com.suchika.wealth.ports.input.CreateInsurancePolicyCommand;
import com.suchika.wealth.ports.input.InsurancePolicyUseCase;
import com.suchika.wealth.ports.input.UpdateInsurancePolicyCommand;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/insurance-policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InsurancePolicyResource {

    private final InsurancePolicyUseCase useCase;

    public InsurancePolicyResource(InsurancePolicyUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listInsurancePolicies(@QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        List<InsurancePolicyResponse> policies = useCase.listInsurancePolicies(adminId).stream()
                .map(InsurancePolicyResponse::from).toList();
        return Response.ok(new ListInsurancePoliciesResponse(policies)).build();
    }

    @POST
    public Response createInsurancePolicy(@QueryParam("admin_id") UUID adminId, CreateInsurancePolicyRequest request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        CreateInsurancePolicyCommand command = new CreateInsurancePolicyCommand(
                request.policyName, request.provider, parsePolicyType(request.policyType),
                request.premiumAmount, parsePremiumFrequency(request.premiumFrequency), request.coverageAmount);
        return Response.status(201)
                .entity(InsurancePolicyResponse.from(useCase.createInsurancePolicy(adminId, command)))
                .build();
    }

    @GET
    @Path("/{insurance_policy_id}")
    public InsurancePolicyResponse getInsurancePolicy(
            @PathParam("insurance_policy_id") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        return InsurancePolicyResponse.from(useCase.getInsurancePolicy(insurancePolicyId, adminId));
    }

    @PATCH
    @Path("/{insurance_policy_id}")
    public InsurancePolicyResponse updateInsurancePolicy(
            @PathParam("insurance_policy_id") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId,
            UpdateInsurancePolicyRequest request) {
        adminId = ResourceUtils.requireAdminId(adminId);
        if (request == null) throw new BadRequestException("Request body is required");
        UpdateInsurancePolicyCommand command = new UpdateInsurancePolicyCommand(
                request.policyName, request.provider, request.premiumAmount,
                parseOptionalPremiumFrequency(request.premiumFrequency), request.coverageAmount,
                request.payoutStructure, request.active);
        return InsurancePolicyResponse.from(useCase.updateInsurancePolicy(insurancePolicyId, adminId, command));
    }

    @DELETE
    @Path("/{insurance_policy_id}")
    public Response deactivateInsurancePolicy(
            @PathParam("insurance_policy_id") UUID insurancePolicyId,
            @QueryParam("admin_id") UUID adminId) {
        adminId = ResourceUtils.requireAdminId(adminId);
        useCase.deactivateInsurancePolicy(insurancePolicyId, adminId);
        return Response.noContent().build();
    }

    private PolicyType parsePolicyType(String value) {
        if (value == null) throw new BadRequestException("policy_type is required");
        try {
            return PolicyType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid policy_type: " + value);
        }
    }

    private PremiumFrequency parsePremiumFrequency(String value) {
        if (value == null) throw new BadRequestException("premium_frequency is required");
        return parseOptionalPremiumFrequency(value);
    }

    private PremiumFrequency parseOptionalPremiumFrequency(String value) {
        if (value == null) return null;
        try {
            return PremiumFrequency.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid premium_frequency: " + value);
        }
    }
}
