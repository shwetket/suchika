package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateProfileRequest {

    @JsonProperty("email_address")
    public String emailAddress;

    @JsonProperty("gender")
    public String gender;

    @JsonProperty("blood_type")
    public String bloodType;

    @JsonProperty("is_active")
    public Boolean isActive;
}
