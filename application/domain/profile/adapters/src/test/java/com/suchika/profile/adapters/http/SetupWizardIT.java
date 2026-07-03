package com.suchika.profile.adapters.http;

import com.suchika.profile.adapters.http.dto.AdminResponse;
import com.suchika.profile.adapters.http.dto.CreateAdminRequest;
import com.suchika.profile.adapters.http.dto.CreateProfileRequest;
import com.suchika.profile.adapters.http.dto.ProfileResponse;
import com.suchika.profile.adapters.http.dto.UpdateAdminPolicyRequest;
import com.suchika.profile.adapters.service.AdminService;
import com.suchika.profile.adapters.service.ProfileService;
import com.suchika.profile.ports.output.AdminRepository;
import com.suchika.profile.ports.output.ProfileRepository;
import com.suchika.shared.exception.ConflictException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * True end-to-end integration test for the Setup Wizard backend flow:
 * POST /v1/admins -> POST /v1/profiles (relation SELF) -> PATCH /v1/admins/{id}/policy.
 * Exercises the full stack in one test: HTTP-shaped resource calls -> real AdminService/
 * ProfileService -> real Panache repositories (@Inject'd) -> real PostgreSQL.
 * Contrast with AdminResourceTest/ProfileResourceTest, which construct the Resource with a
 * hand-written StubUseCase and never touch the DB. This test wires the real, DI-managed
 * services and repositories, then calls Resource methods directly — the same style
 * ProfileResourceTest uses for calling conventions, but backed by the real DB stack.
 * <p>
 * Requires a running local PostgreSQL (app_db) with the profile schema migrated.
 * Each test creates its own Admin row (unique email) so it never assumes an empty table
 * and never touches the seeded Admin/Profile rows or any other manually-tested data.
 * Runs in a transaction that is rolled back on completion (@TestTransaction).
 */
@QuarkusTest
@TestTransaction
class SetupWizardIT {

    @Inject
    AdminRepository adminRepository;

    @Inject
    ProfileRepository profileRepository;

    private AdminResource adminResource;
    private ProfileResource profileResource;

    @BeforeEach
    void setUp() {
        AdminService adminService = new AdminService(adminRepository, profileRepository);
        ProfileService profileService = new ProfileService(profileRepository, adminRepository);
        adminResource = new AdminResource(adminService);
        profileResource = new ProfileResource(profileService);
    }

    @Test
    void setupWizard_createAdminThenSelfProfileThenPolicy_happyPath() {
        String uniqueEmail = "setup-wizard-" + System.nanoTime() + "@test.com";

        CreateAdminRequest adminRequest = new CreateAdminRequest();
        adminRequest.displayName = "Wizard Admin";
        adminRequest.emailAddress = uniqueEmail;
        Response adminHttpResponse = adminResource.createAdmin(adminRequest);
        assertEquals(201, adminHttpResponse.getStatus());
        AdminResponse createdAdmin = (AdminResponse) adminHttpResponse.getEntity();
        assertEquals("Wizard Admin", createdAdmin.displayName);
        UUID adminId = createdAdmin.adminId;
        assertNotNull(adminId);

        CreateProfileRequest profileRequest = new CreateProfileRequest();
        profileRequest.adminId = adminId;
        profileRequest.fullName = "Wizard Self";
        profileRequest.dob = LocalDate.of(1990, Month.MAY, 20);
        profileRequest.relationToAdmin = "SELF";
        Response profileHttpResponse = profileResource.createProfile(profileRequest);
        assertEquals(201, profileHttpResponse.getStatus());
        ProfileResponse createdProfile = (ProfileResponse) profileHttpResponse.getEntity();
        assertEquals("SELF", createdProfile.relationToAdmin);
        UUID profileId = createdProfile.profileId;
        assertNotNull(profileId);

        UpdateAdminPolicyRequest policyRequest = new UpdateAdminPolicyRequest();
        policyRequest.policySettings = Map.of("setup_completed", "true");
        AdminResponse updatedAdmin = adminResource.updatePolicySettings(adminId, policyRequest);
        assertEquals("true", updatedAdmin.policySettings.get("setup_completed"));

        // Round-trip verify: profile is retrievable via GET, proving it landed in real Postgres
        ProfileResponse fetched = profileResource.getProfile(profileId);
        assertEquals("Wizard Self", fetched.fullName);
        assertEquals(adminId, fetched.adminId);
    }

    @Test
    void setupWizard_duplicateSelfProfileForSameAdmin_returns409Conflict() {
        String uniqueEmail = "setup-wizard-dup-" + System.nanoTime() + "@test.com";

        CreateAdminRequest adminRequest = new CreateAdminRequest();
        adminRequest.displayName = "Dup Admin";
        adminRequest.emailAddress = uniqueEmail;
        Response adminHttpResponse = adminResource.createAdmin(adminRequest);
        UUID adminId = ((AdminResponse) adminHttpResponse.getEntity()).adminId;

        CreateProfileRequest firstSelf = new CreateProfileRequest();
        firstSelf.adminId = adminId;
        firstSelf.fullName = "First Self";
        firstSelf.dob = LocalDate.of(1990, Month.MAY, 20);
        firstSelf.relationToAdmin = "SELF";
        Response firstResponse = profileResource.createProfile(firstSelf);
        assertEquals(201, firstResponse.getStatus());

        // Second SELF profile for the same admin must be rejected with 409, not silently accepted
        CreateProfileRequest secondSelf = new CreateProfileRequest();
        secondSelf.adminId = adminId;
        secondSelf.fullName = "Second Self";
        secondSelf.dob = LocalDate.of(1991, Month.JANUARY, 1);
        secondSelf.relationToAdmin = "SELF";

        ConflictException ex = assertThrows(ConflictException.class,
                () -> profileResource.createProfile(secondSelf));
        assertEquals(409, ex.getStatusCode());
    }
}
