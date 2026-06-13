package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.LocalDate;
import java.util.UUID;

@RegisterForReflection
public class CreateProfileRequest {

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
}
