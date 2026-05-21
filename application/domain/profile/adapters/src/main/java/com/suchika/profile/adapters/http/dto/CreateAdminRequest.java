package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CreateAdminRequest {

    @JsonProperty("display_name")
    public String displayName;

    @JsonProperty("email_address")
    public String emailAddress;
}
