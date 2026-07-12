package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.CreateInsurancePolicyRequest;
import com.suchika.wealth.adapters.http.dto.InsurancePolicyResponse;
import com.suchika.wealth.adapters.http.dto.ListInsurancePoliciesResponse;
import com.suchika.wealth.adapters.http.dto.UpdateInsurancePolicyRequest;
import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.domain.PolicyType;
import com.suchika.wealth.domain.PremiumFrequency;
import com.suchika.wealth.ports.input.CreateInsurancePolicyCommand;
import com.suchika.wealth.ports.input.InsurancePolicyUseCase;
import com.suchika.wealth.ports.input.UpdateInsurancePolicyCommand;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsurancePolicyResourceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();

    private InsurancePolicyResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new InsurancePolicyResource(useCase);
    }

    @Test
    void listInsurancePolicies_returns200_withPolicyList() {
        useCase.policiesToReturn = List.of(buildPolicy());

        Response response = resource.listInsurancePolicies(ADMIN_ID);

        assertEquals(200, response.getStatus());
        ListInsurancePoliciesResponse body = (ListInsurancePoliciesResponse) response.getEntity();
        assertEquals(1, body.insurancePolicies.size());
        assertEquals(ADMIN_ID, useCase.lastListAdminId);
    }

    @Test
    void listInsurancePolicies_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listInsurancePolicies(null));
    }

    @Test
    void createInsurancePolicy_returns201_withCreatedPolicy() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest();
        request.policyName = "Term Plan";
        request.provider = "HDFC Life";
        request.policyType = "TERM";
        request.premiumAmount = new BigDecimal("1500");
        request.premiumFrequency = "MONTHLY";
        useCase.policyToReturn = buildPolicy();

        Response response = resource.createInsurancePolicy(ADMIN_ID, request);

        assertEquals(201, response.getStatus());
        assertEquals(PolicyType.TERM, useCase.lastCreateCommand.policyType());
        assertEquals(PremiumFrequency.MONTHLY, useCase.lastCreateCommand.premiumFrequency());
        assertEquals("Term Plan", useCase.lastCreateCommand.policyName());
    }

    @Test
    void createInsurancePolicy_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.createInsurancePolicy(ADMIN_ID, null));
    }

    @Test
    void createInsurancePolicy_missingAdminId_throwsBadRequest() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest();
        request.policyName = "Term Plan";
        request.provider = "HDFC Life";
        request.policyType = "TERM";
        request.premiumAmount = new BigDecimal("1500");
        request.premiumFrequency = "MONTHLY";
        assertThrows(BadRequestException.class, () -> resource.createInsurancePolicy(null, request));
    }

    @Test
    void createInsurancePolicy_invalidPolicyType_throwsBadRequest() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest();
        request.policyName = "Term Plan";
        request.provider = "HDFC Life";
        request.policyType = "NOT_A_TYPE";
        request.premiumAmount = new BigDecimal("1500");
        request.premiumFrequency = "MONTHLY";
        assertThrows(BadRequestException.class, () -> resource.createInsurancePolicy(ADMIN_ID, request));
    }

    @Test
    void createInsurancePolicy_invalidPremiumFrequency_throwsBadRequest() {
        CreateInsurancePolicyRequest request = new CreateInsurancePolicyRequest();
        request.policyName = "Term Plan";
        request.provider = "HDFC Life";
        request.policyType = "TERM";
        request.premiumAmount = new BigDecimal("1500");
        request.premiumFrequency = "FORTNIGHTLY";
        assertThrows(BadRequestException.class, () -> resource.createInsurancePolicy(ADMIN_ID, request));
    }

    @Test
    void getInsurancePolicy_returnsInsurancePolicyResponse() {
        useCase.policyToReturn = buildPolicy();

        InsurancePolicyResponse response = resource.getInsurancePolicy(POLICY_ID, ADMIN_ID);

        assertEquals("TERM", response.policyType);
        assertEquals(ADMIN_ID, useCase.lastGetAdminId);
    }

    @Test
    void getInsurancePolicy_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.getInsurancePolicy(POLICY_ID, null));
    }

    @Test
    void updateInsurancePolicy_returnsUpdatedPolicy() {
        UpdateInsurancePolicyRequest request = new UpdateInsurancePolicyRequest();
        request.policyName = "Updated Plan";
        useCase.policyToReturn = buildPolicy();

        InsurancePolicyResponse response = resource.updateInsurancePolicy(POLICY_ID, ADMIN_ID, request);

        assertEquals("TERM", response.policyType);
        assertEquals(POLICY_ID, useCase.lastUpdatePolicyId);
        assertEquals("Updated Plan", useCase.lastUpdateCommand.policyName());
    }

    @Test
    void updateInsurancePolicy_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updateInsurancePolicy(POLICY_ID, ADMIN_ID, null));
    }

    @Test
    void deactivateInsurancePolicy_returns204() {
        Response response = resource.deactivateInsurancePolicy(POLICY_ID, ADMIN_ID);

        assertEquals(204, response.getStatus());
        assertEquals(POLICY_ID, useCase.lastDeactivatePolicyId);
        assertEquals(ADMIN_ID, useCase.lastDeactivateAdminId);
    }

    @Test
    void deactivateInsurancePolicy_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.deactivateInsurancePolicy(POLICY_ID, null));
    }

    private InsurancePolicy buildPolicy() {
        return InsurancePolicy.builder()
                .id(POLICY_ID)
                .adminId(ADMIN_ID)
                .policyName("Term Plan")
                .provider("HDFC Life")
                .policyType(PolicyType.TERM)
                .premiumAmount(new BigDecimal("1500"))
                .premiumFrequency(PremiumFrequency.MONTHLY)
                .active(true)
                .build();
    }

    static class StubUseCase implements InsurancePolicyUseCase {
        List<InsurancePolicy> policiesToReturn = List.of();
        InsurancePolicy policyToReturn;

        UUID lastListAdminId;
        CreateInsurancePolicyCommand lastCreateCommand;
        UUID lastGetAdminId;
        UUID lastUpdatePolicyId;
        UpdateInsurancePolicyCommand lastUpdateCommand;
        UUID lastDeactivatePolicyId;
        UUID lastDeactivateAdminId;

        @Override
        public InsurancePolicy createInsurancePolicy(UUID adminId, CreateInsurancePolicyCommand command) {
            lastCreateCommand = command;
            return policyToReturn;
        }

        @Override
        public InsurancePolicy getInsurancePolicy(UUID id, UUID adminId) {
            lastGetAdminId = adminId;
            return policyToReturn;
        }

        @Override
        public List<InsurancePolicy> listInsurancePolicies(UUID adminId) {
            lastListAdminId = adminId;
            return policiesToReturn;
        }

        @Override
        public InsurancePolicy updateInsurancePolicy(UUID id, UUID adminId, UpdateInsurancePolicyCommand command) {
            lastUpdatePolicyId = id;
            lastUpdateCommand = command;
            return policyToReturn;
        }

        @Override
        public void deactivateInsurancePolicy(UUID id, UUID adminId) {
            lastDeactivatePolicyId = id;
            lastDeactivateAdminId = adminId;
        }
    }
}
