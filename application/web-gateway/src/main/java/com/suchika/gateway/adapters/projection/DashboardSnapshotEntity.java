package com.suchika.gateway.adapters.projection;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for projections.dashboard_snapshot.
 *
 * <p>Placed in the gateway adapters package so ArchUnit's rule
 * "jpa_entities_must_only_reside_in_adapters" is satisfied.
 */
@Entity
@Table(name = "dashboard_snapshot", schema = "projections")
@IdClass(DashboardSnapshotEntity.DashboardSnapshotId.class)
public class DashboardSnapshotEntity {

    @Id
    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Id
    @Column(name = "snapshot_key", nullable = false, length = 100)
    private String snapshotKey;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected DashboardSnapshotEntity() {
    }

    public DashboardSnapshotEntity(UUID profileId, String snapshotKey, String payload, Instant calculatedAt) {
        this.profileId = profileId;
        this.snapshotKey = snapshotKey;
        this.payload = payload;
        this.calculatedAt = calculatedAt;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getSnapshotKey() {
        return snapshotKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    /**
     * Composite PK class for (profile_id, snapshot_key).
     */
    public static class DashboardSnapshotId implements Serializable {

        private UUID profileId;
        private String snapshotKey;

        public DashboardSnapshotId() {
        }

        public DashboardSnapshotId(UUID profileId, String snapshotKey) {
            this.profileId = profileId;
            this.snapshotKey = snapshotKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DashboardSnapshotId)) return false;
            DashboardSnapshotId that = (DashboardSnapshotId) o;
            return profileId != null && profileId.equals(that.profileId)
                    && snapshotKey != null && snapshotKey.equals(that.snapshotKey);
        }

        @Override
        public int hashCode() {
            int result = profileId != null ? profileId.hashCode() : 0;
            result = 31 * result + (snapshotKey != null ? snapshotKey.hashCode() : 0);
            return result;
        }
    }
}
