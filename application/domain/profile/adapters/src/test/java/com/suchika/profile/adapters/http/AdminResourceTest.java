package com.suchika.profile.adapters.http;

import com.suchika.profile.adapters.http.dto.AdminResponse;
import com.suchika.profile.adapters.http.dto.CreateAdminRequest;
import com.suchika.profile.adapters.http.dto.ListAdminsResponse;
import com.suchika.profile.adapters.http.dto.UpdateAdminPolicyRequest;
import com.suchika.profile.adapters.http.dto.UpdateAdminRequest;
import com.suchika.profile.domain.Admin;
import com.suchika.profile.ports.input.AdminUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminResourceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private AdminResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new AdminResource(useCase);
    }

    @Test
    void listAdmins_returnsAdminList() {
        useCase.adminsToReturn = List.of(buildAdmin());

        ListAdminsResponse response = resource.listAdmins();

        assertEquals(1, response.admins.size());
        assertEquals("Ketan", response.admins.get(0).displayName);
    }

    @Test
    void createAdmin_returns201_withCreatedAdmin() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.displayName = "Ketan";
        request.emailAddress = "ketan@example.com";
        useCase.adminToReturn = buildAdmin();

        Response response = resource.createAdmin(request);

        assertEquals(201, response.getStatus());
        assertEquals("Ketan", useCase.lastCreateDisplayName);
        AdminResponse body = (AdminResponse) response.getEntity();
        assertEquals("Ketan", body.displayName);
    }

    @Test
    void createAdmin_blankDisplayName_throwsBadRequest() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.displayName = "  ";
        assertThrows(BadRequestException.class, () -> resource.createAdmin(request));
    }

    @Test
    void getAdmin_returnsAdminResponse() {
        useCase.adminToReturn = buildAdmin();

        AdminResponse response = resource.getAdmin(ADMIN_ID);

        assertEquals("Ketan", response.displayName);
    }

    @Test
    void updateAdmin_returnsUpdatedAdmin() {
        UpdateAdminRequest request = new UpdateAdminRequest();
        request.displayName = "Ketan V";
        useCase.adminToReturn = buildAdmin();

        AdminResponse response = resource.updateAdmin(ADMIN_ID, request);

        assertEquals("Ketan", response.displayName);
        assertEquals(ADMIN_ID, useCase.lastUpdateAdminId);
    }

    @Test
    void deactivateAdmin_returns204() {
        Response response = resource.deactivateAdmin(ADMIN_ID);

        assertEquals(204, response.getStatus());
        assertEquals(ADMIN_ID, useCase.lastDeactivateAdminId);
    }

    @Test
    void updatePolicySettings_returnsUpdatedAdmin() {
        UpdateAdminPolicyRequest request = new UpdateAdminPolicyRequest();
        request.policySettings = Map.of("monthly_budget_cap", "100000");
        useCase.adminToReturn = buildAdmin();

        AdminResponse response = resource.updatePolicySettings(ADMIN_ID, request);

        assertEquals("Ketan", response.displayName);
        assertEquals("100000", useCase.lastPolicySettings.get("monthly_budget_cap"));
    }

    @Test
    void updatePolicySettings_nullSettings_throwsBadRequest() {
        UpdateAdminPolicyRequest request = new UpdateAdminPolicyRequest();
        assertThrows(BadRequestException.class, () -> resource.updatePolicySettings(ADMIN_ID, request));
    }

    @Test
    void updatePolicySettings_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.updatePolicySettings(ADMIN_ID, null));
    }

    private Admin buildAdmin() {
        return new Admin(ADMIN_ID, "Ketan", "ketan@example.com", true, null, null);
    }

    static class StubUseCase implements AdminUseCase {
        List<Admin> adminsToReturn = List.of();
        Admin adminToReturn;

        String lastCreateDisplayName;
        UUID lastUpdateAdminId;
        UUID lastDeactivateAdminId;
        Map<String, String> lastPolicySettings;

        @Override
        public Admin createAdmin(String displayName, String emailAddress) {
            lastCreateDisplayName = displayName;
            return adminToReturn;
        }

        @Override
        public Admin getAdmin(UUID adminId) {
            return adminToReturn;
        }

        @Override
        public List<Admin> listAdmins() {
            return adminsToReturn;
        }

        @Override
        public Admin updateAdmin(UUID adminId, String displayName, String emailAddress, Boolean isActive) {
            lastUpdateAdminId = adminId;
            return adminToReturn;
        }

        @Override
        public void deactivateAdmin(UUID adminId) {
            lastDeactivateAdminId = adminId;
        }

        @Override
        public Admin updatePolicySettings(UUID adminId, Map<String, String> settings) {
            lastPolicySettings = settings;
            return adminToReturn;
        }
    }
}
