package com.suchika.profile.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.profile.domain.Admin;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminResponse {

    @JsonProperty("name")
    public String name;

    @JsonProperty("admin_id")
    public UUID adminId;

    @JsonProperty("display_name")
    public String displayName;

    @JsonProperty("email_address")
    public String emailAddress;

    @JsonProperty("is_active")
    public boolean active;

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("policy_settings")
    public Map<String, String> policySettings;

    public static AdminResponse from(Admin admin) {
        AdminResponse r = new AdminResponse();
        r.name = "admins/" + admin.getId();
        r.adminId = admin.getId();
        r.displayName = admin.getDisplayName();
        r.emailAddress = admin.getEmailAddress();
        r.active = admin.isActive();
        r.createdAt = admin.getCreatedAt();
        r.policySettings = admin.getPolicySettings();
        return r;
    }
}
