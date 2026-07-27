package com.suchika.profile.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Admin {

    private UUID id;
    private String displayName;
    private String emailAddress;
    private boolean active;
    private Instant createdAt;
    private Map<String, String> policySettings;

    public Admin() {
        this.policySettings = new HashMap<>();
    }

    public Admin(UUID id, String displayName, String emailAddress, boolean active, Instant createdAt,
                 Map<String, String> policySettings) {
        this.id = id;
        this.displayName = displayName;
        this.emailAddress = emailAddress;
        this.active = active;
        this.createdAt = createdAt;
        this.policySettings = policySettings != null ? policySettings : new HashMap<>();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getPolicySettings() { return policySettings; }
    public void setPolicySettings(Map<String, String> policySettings) {
        this.policySettings = policySettings != null ? policySettings : new HashMap<>();
    }
}
