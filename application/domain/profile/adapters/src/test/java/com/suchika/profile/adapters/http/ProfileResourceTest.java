package com.suchika.profile.adapters.http;

import com.suchika.profile.adapters.http.dto.CreateProfileRequest;
import com.suchika.profile.adapters.http.dto.ListProfilesResponse;
import com.suchika.profile.adapters.http.dto.ProfileResponse;
import com.suchika.profile.adapters.http.dto.UpdateProfileRequest;
import com.suchika.profile.domain.BloodType;
import com.suchika.profile.domain.Gender;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.domain.RelationToAdmin;
import com.suchika.profile.ports.input.ProfileUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileResourceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID PROFILE_ID = UUID.randomUUID();

    private ProfileResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new ProfileResource(useCase);
    }

    @Test
    void listProfiles_returnsProfileList() {
        useCase.profilesToReturn = List.of(buildProfile());

        ListProfilesResponse response = resource.listProfiles(ADMIN_ID, true);

        assertEquals(1, response.profiles.size());
        assertEquals("Ketan", response.profiles.get(0).fullName);
    }

    @Test
    void listProfiles_missingAdminId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listProfiles(null, true));
    }

    @Test
    void createProfile_returns201_withCreatedProfile() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.adminId = ADMIN_ID;
        request.fullName = "Ketan";
        request.dob = LocalDate.of(1990, Month.MAY, 20);
        request.relationToAdmin = "SELF";
        useCase.profileToReturn = buildProfile();

        Response response = resource.createProfile(request);

        assertEquals(201, response.getStatus());
        assertEquals(RelationToAdmin.SELF, useCase.lastRelation);
        ProfileResponse body = (ProfileResponse) response.getEntity();
        assertEquals("Ketan", body.fullName);
    }

    @Test
    void createProfile_missingAdminId_throwsBadRequest() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.fullName = "Ketan";
        request.dob = LocalDate.of(1990, Month.MAY, 20);
        request.relationToAdmin = "SELF";
        assertThrows(BadRequestException.class, () -> resource.createProfile(request));
    }

    @Test
    void createProfile_invalidRelation_throwsBadRequest() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.adminId = ADMIN_ID;
        request.fullName = "Ketan";
        request.dob = LocalDate.of(1990, Month.MAY, 20);
        request.relationToAdmin = "NOT_A_RELATION";
        assertThrows(BadRequestException.class, () -> resource.createProfile(request));
    }

    @Test
    void createProfile_invalidBloodType_throwsBadRequest() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.adminId = ADMIN_ID;
        request.fullName = "Ketan";
        request.dob = LocalDate.of(1990, Month.MAY, 20);
        request.relationToAdmin = "SELF";
        request.bloodType = "NOT_A_BLOOD_TYPE";
        assertThrows(BadRequestException.class, () -> resource.createProfile(request));
    }

    @Test
    void getProfile_returnsProfileResponse() {
        useCase.profileToReturn = buildProfile();

        ProfileResponse response = resource.getProfile(PROFILE_ID);

        assertEquals("Ketan", response.fullName);
    }

    @Test
    void updateProfile_returnsUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.emailAddress = "new@example.com";
        useCase.profileToReturn = buildProfile();

        ProfileResponse response = resource.updateProfile(PROFILE_ID, request);

        assertEquals("Ketan", response.fullName);
        assertEquals(PROFILE_ID, useCase.lastUpdateProfileId);
    }

    @Test
    void updateProfile_invalidGender_throwsBadRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.gender = "NOT_A_GENDER";
        assertThrows(BadRequestException.class, () -> resource.updateProfile(PROFILE_ID, request));
    }

    @Test
    void deactivateProfile_returns204() {
        Response response = resource.deactivateProfile(PROFILE_ID);

        assertEquals(204, response.getStatus());
        assertEquals(PROFILE_ID, useCase.lastDeactivateProfileId);
    }

    private Profile buildProfile() {
        return Profile.builder()
                .id(PROFILE_ID)
                .adminId(ADMIN_ID)
                .fullName("Ketan")
                .dob(LocalDate.of(1990, Month.MAY, 20))
                .relationToAdmin(RelationToAdmin.SELF)
                .active(true)
                .build();
    }

    static class StubUseCase implements ProfileUseCase {
        List<Profile> profilesToReturn = List.of();
        Profile profileToReturn;

        RelationToAdmin lastRelation;
        UUID lastUpdateProfileId;
        UUID lastDeactivateProfileId;

        @Override
        public Profile createProfile(UUID adminId, String fullName, LocalDate dob,
                                      RelationToAdmin relationToAdmin, String emailAddress,
                                      Gender gender, BloodType bloodType) {
            lastRelation = relationToAdmin;
            return profileToReturn;
        }

        @Override
        public Profile getProfile(UUID profileId) {
            return profileToReturn;
        }

        @Override
        public List<Profile> listProfiles(UUID adminId, Boolean isActive) {
            return profilesToReturn;
        }

        @Override
        public Profile updateProfile(UUID profileId, String emailAddress, Gender gender,
                                      BloodType bloodType, Boolean isActive) {
            lastUpdateProfileId = profileId;
            return profileToReturn;
        }

        @Override
        public void deactivateProfile(UUID profileId) {
            lastDeactivateProfileId = profileId;
        }
    }
}
