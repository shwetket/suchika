package com.suchika.profile.adapters.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.profile.domain.Admin;
import com.suchika.shared.logging.AppLogger;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin", schema = "profile")
public class AdminEntity extends PanacheEntityBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "display_name", nullable = false, length = 150)
    public String displayName;

    @Column(name = "email_address", length = 255)
    public String emailAddress;

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_settings", columnDefinition = "jsonb", nullable = false)
    public String policySettings = "{}";

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static AdminEntity from(Admin admin) {
        AdminEntity e = new AdminEntity();
        e.id = admin.getId();
        e.displayName = admin.getDisplayName();
        e.emailAddress = admin.getEmailAddress();
        e.active = admin.isActive();
        e.createdAt = admin.getCreatedAt();
        e.policySettings = writePolicySettings(admin.getPolicySettings());
        return e;
    }

    public Admin toDomain() {
        return new Admin(id, displayName, emailAddress, active, createdAt,
                readPolicySettings(policySettings));
    }

    private static String writePolicySettings(Map<String, String> settings) {
        if (settings == null || settings.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(settings);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            AppLogger.warn("Failed to serialize policy_settings, defaulting to empty object: %s", e.getMessage());
            return "{}";
        }
    }

    private static Map<String, String> readPolicySettings(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(json, MAP_TYPE);
        } catch (java.io.IOException e) {
            AppLogger.warn("Failed to deserialize policy_settings, defaulting to empty map: %s", e.getMessage());
            return new HashMap<>();
        }
    }
}
