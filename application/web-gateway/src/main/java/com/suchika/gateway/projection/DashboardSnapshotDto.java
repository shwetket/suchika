package com.suchika.gateway.projection;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight read DTO for a single dashboard snapshot row.
 * Payload is kept as a raw JSON string — the frontend parses the shape.
 * Snake-case property names match the gateway OpenAPI contract.
 */
public class DashboardSnapshotDto {

    private final UUID profileId;
    private final String snapshotKey;
    private final String payload;
    private final Instant calculatedAt;

    public DashboardSnapshotDto(UUID profileId, String snapshotKey, String payload, Instant calculatedAt) {
        this.profileId = profileId;
        this.snapshotKey = snapshotKey;
        this.payload = payload;
        this.calculatedAt = calculatedAt;
    }

    @JsonProperty("profile_id")
    public UUID getProfileId() {
        return profileId;
    }

    @JsonProperty("snapshot_key")
    public String getSnapshotKey() {
        return snapshotKey;
    }

    public String getPayload() {
        return payload;
    }

    @JsonProperty("calculated_at")
    public Instant getCalculatedAt() {
        return calculatedAt;
    }
}
