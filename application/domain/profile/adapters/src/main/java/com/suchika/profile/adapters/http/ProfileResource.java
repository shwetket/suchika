package com.suchika.profile.adapters.http;

import com.suchika.profile.adapters.http.dto.*;
import com.suchika.profile.domain.BloodType;
import com.suchika.profile.domain.Gender;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.domain.RelationToAdmin;
import com.suchika.profile.ports.input.ProfileUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    private final ProfileUseCase profileUseCase;

    public ProfileResource(ProfileUseCase profileUseCase) {
        this.profileUseCase = profileUseCase;
    }

    @GET
    public ListProfilesResponse listProfiles(
            @QueryParam("admin_id") UUID adminId,
            @QueryParam("is_active") Boolean isActive) {
        List<ProfileResponse> profiles = profileUseCase.listProfiles(adminId, isActive)
            .stream().map(ProfileResponse::from).toList();
        return new ListProfilesResponse(profiles);
    }

    @POST
    public Response createProfile(CreateProfileRequest request) {
        if (request.adminId == null) throw new BadRequestException("admin_id is required");
        if (request.fullName == null || request.fullName.isBlank()) throw new BadRequestException("full_name is required");
        if (request.dob == null) throw new BadRequestException("dob is required");
        if (request.relationToAdmin == null) throw new BadRequestException("relation_to_admin is required");

        RelationToAdmin relation = parseEnum(RelationToAdmin.class, request.relationToAdmin, "relation_to_admin");
        Gender gender = request.gender != null ? parseEnum(Gender.class, request.gender, "gender") : null;
        BloodType bloodType = request.bloodType != null ? parseBloodType(request.bloodType) : null;

        Profile profile = profileUseCase.createProfile(
            request.adminId, request.fullName, request.dob,
            relation, request.emailAddress, gender, bloodType
        );
        return Response.status(201).entity(ProfileResponse.from(profile)).build();
    }

    @GET
    @Path("/{profile_id}")
    public ProfileResponse getProfile(@PathParam("profile_id") UUID profileId) {
        return ProfileResponse.from(profileUseCase.getProfile(profileId));
    }

    @PATCH
    @Path("/{profile_id}")
    public ProfileResponse updateProfile(@PathParam("profile_id") UUID profileId, UpdateProfileRequest request) {
        Gender gender = request.gender != null ? parseEnum(Gender.class, request.gender, "gender") : null;
        BloodType bloodType = request.bloodType != null ? parseBloodType(request.bloodType) : null;
        Profile profile = profileUseCase.updateProfile(profileId, request.emailAddress, gender, bloodType, request.isActive);
        return ProfileResponse.from(profile);
    }

    @DELETE
    @Path("/{profile_id}")
    public Response deactivateProfile(@PathParam("profile_id") UUID profileId) {
        profileUseCase.deactivateProfile(profileId);
        return Response.noContent().build();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String fieldName) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid value for " + fieldName + ": " + value);
        }
    }

    private BloodType parseBloodType(String value) {
        try {
            return BloodType.fromLabel(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid value for blood_type: " + value);
        }
    }
}
