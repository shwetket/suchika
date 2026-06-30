package com.suchika.gateway.projection;

/**
 * Well-known snapshot keys for the projections.dashboard_snapshot table.
 * One constant per metric type — adding a new metric requires a new constant here
 * and a new compute method in ProjectionCalculationEngine.
 */
public final class SnapshotKey {

    public static final String WEALTH_NET_WORTH = "WEALTH_NET_WORTH";
    public static final String WEALTH_GOAL_PROGRESS = "WEALTH_GOAL_PROGRESS";
    public static final String HEALTH_VITALS_SUMMARY = "HEALTH_VITALS_SUMMARY";
    public static final String HOUSEHOLD_EVENT_SUMMARY = "HOUSEHOLD_EVENT_SUMMARY";

    /**
     * Epic 8 Phase 1 validation seed (Use Case 8.4, narrow scope): every account
     * resolves to exactly one classification category; flags accounts that don't.
     * Since wealth.account.metadata.category is not populated until Phase 2, this
     * check is EXPECTED to flag every account as uncategorized for now — that is
     * correct, not a bug. See documents/EPIC8_IMPLEMENTATION_PLAN.md Phase 1.
     */
    public static final String WEALTH_CATEGORY_VALIDATION = "WEALTH_CATEGORY_VALIDATION";

    /**
     * Household-level rollup keys (ADR-017). Stored under the admin's own profile_id,
     * not a separate admin-keyed row — same (profile_id, snapshot_key) shape as every
     * other snapshot. Payload nests a per-member breakdown in a "members" array.
     *
     * Only WEALTH_NET_WORTH_FAMILY has a working compute method as of Phase 1.
     * The other three are reserved keys for later phases (goals engine, validation
     * engine, EMI tracking) and are not yet populated by any compute method.
     */
    public static final String WEALTH_NET_WORTH_FAMILY = "WEALTH_NET_WORTH_FAMILY";
    public static final String WEALTH_GOAL_PROGRESS_FAMILY = "WEALTH_GOAL_PROGRESS_FAMILY";
    public static final String WEALTH_VALIDATION_REPORT_FAMILY = "WEALTH_VALIDATION_REPORT_FAMILY";
    public static final String WEALTH_EMI_TRACKING_FAMILY = "WEALTH_EMI_TRACKING_FAMILY";

    private SnapshotKey() {
    }
}
