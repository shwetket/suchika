package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
public class UpdateAdminPolicyRequest {

    @JsonProperty("policy_settings")
    public Map<String, String> policySettings;
}
