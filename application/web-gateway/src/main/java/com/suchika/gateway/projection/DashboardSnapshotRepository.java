package com.suchika.gateway.projection;

import com.suchika.gateway.adapters.projection.DashboardSnapshotEntity;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Repository for projections.dashboard_snapshot.
 *
 * <p>All writes go through a native UPSERT to guarantee idempotency:
 * running refreshAll() multiple times for the same profile leaves exactly
 * one row per (profile_id, snapshot_key).
 *
 * <p>{@code Instance<EntityManager>} is used instead of a direct injection so
 * that the CDI wiring compiles when Hibernate ORM is disabled in the test
 * profile (the injection point is always satisfiable; the Instance is empty
 * when Hibernate is off and the bean is @InjectMock'd in tests anyway).
 */
@ApplicationScoped
public class DashboardSnapshotRepository {

    private final Instance<EntityManager> emInstance;

    @Inject
    public DashboardSnapshotRepository(Instance<EntityManager> emInstance) {
        this.emInstance = emInstance;
    }

    private EntityManager em() {
        return emInstance.get();
    }

    /**
     * UPSERT a snapshot. Safe to call repeatedly — existing row is overwritten.
     */
    @Transactional
    public void upsert(UUID profileId, String snapshotKey, String jsonPayload) {
        AppLogger.info("ProjectionRepo: upserting snapshot %s for profile %s", snapshotKey, profileId);
        em().createNativeQuery(
                "INSERT INTO projections.dashboard_snapshot (profile_id, snapshot_key, payload, calculated_at) "
                + "VALUES (:profileId, :snapshotKey, CAST(:payload AS jsonb), now()) "
                + "ON CONFLICT (profile_id, snapshot_key) DO UPDATE "
                + "SET payload = EXCLUDED.payload, calculated_at = now()")
                .setParameter("profileId", profileId)
                .setParameter("snapshotKey", snapshotKey)
                .setParameter("payload", jsonPayload)
                .executeUpdate();
    }

    /**
     * Read all snapshots for a profile. Returns empty list when no refresh has run yet.
     */
    public List<DashboardSnapshotDto> findByProfileId(UUID profileId) {
        List<DashboardSnapshotEntity> entities = em()
                .createQuery(
                        "SELECT d FROM DashboardSnapshotEntity d WHERE d.profileId = :profileId",
                        DashboardSnapshotEntity.class)
                .setParameter("profileId", profileId)
                .getResultList();

        return entities.stream()
                .map(e -> new DashboardSnapshotDto(
                        e.getProfileId(),
                        e.getSnapshotKey(),
                        e.getPayload(),
                        e.getCalculatedAt()))
                .toList();
    }
}
