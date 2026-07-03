package com.suchika.profile.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTest {

    @Test
    void noArgConstructor_initializesEmptyPolicySettings() {
        Admin admin = new Admin();

        assertNotNull(admin.getPolicySettings());
        assertTrue(admin.getPolicySettings().isEmpty());
    }

    @Test
    void fullConstructor_withPolicySettings_preservesProvidedMap() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Map<String, String> settings = Map.of("monthly_budget_cap", "100000");

        Admin admin = new Admin(id, "Ketan", "ketan@example.com", true, createdAt, settings);

        assertEquals(id, admin.getId());
        assertEquals("Ketan", admin.getDisplayName());
        assertEquals("100000", admin.getPolicySettings().get("monthly_budget_cap"));
    }

    @Test
    void fullConstructor_withNullPolicySettings_defaultsToEmptyMap() {
        Admin admin = new Admin(UUID.randomUUID(), "Ketan", "ketan@example.com", true,
                Instant.parse("2026-01-01T00:00:00Z"), null);

        assertNotNull(admin.getPolicySettings());
        assertTrue(admin.getPolicySettings().isEmpty());
    }

    @Test
    void setPolicySettings_null_resetsToEmptyMapRatherThanStoringNull() {
        Admin admin = new Admin();
        admin.setPolicySettings(new HashMap<>(Map.of("debt_crossover_threshold_percent", "50")));

        admin.setPolicySettings(null);

        assertNotNull(admin.getPolicySettings());
        assertTrue(admin.getPolicySettings().isEmpty());
    }

    @Test
    void setters_updateFieldsAfterConstruction() {
        Admin admin = new Admin();
        UUID id = UUID.randomUUID();

        admin.setId(id);
        admin.setDisplayName("Ketan Verma");
        admin.setActive(true);

        assertEquals(id, admin.getId());
        assertEquals("Ketan Verma", admin.getDisplayName());
        assertEquals(true, admin.isActive());
    }
}
