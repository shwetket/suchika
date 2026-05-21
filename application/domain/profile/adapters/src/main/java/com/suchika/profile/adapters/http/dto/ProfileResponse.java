package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.profile.domain.Profile;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    @JsonProperty("name")
    public String name;

    @JsonProperty("profile_id")
    public UUID profileId;

    @JsonProperty("admin_id")
    public UUID adminId;

    @JsonProperty("full_name")
    public String fullName;

    @JsonProperty("dob")
    public LocalDate dob;

    @JsonProperty("relation_to_admin")
    public String relationToAdmin;

    @JsonProperty("email_address")
    public String emailAddress;

    @JsonProperty("gender")
    public String gender;

    @JsonProperty("blood_type")
    public String bloodType;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("created_at")
    public Instant createdAt;

    public static ProfileResponse from(Profile profile) {
        ProfileResponse r = new ProfileResponse();
        r.name = "profiles/" + profile.getId();
        r.profileId = profile.getId();
        r.adminId = profile.getAdminId();
        r.fullName = profile.getFullName();
        r.dob = profile.getDob();
        r.relationToAdmin = profile.getRelationToAdmin() != null ? profile.getRelationToAdmin().name() : null;
        r.emailAddress = profile.getEmailAddress();
        r.gender = profile.getGender() != null ? profile.getGender().name() : null;
        r.bloodType = profile.getBloodType() != null ? profile.getBloodType().getLabel() : null;
        r.active = profile.isActive();
        r.createdAt = profile.getCreatedAt();
        return r;
    }
}
