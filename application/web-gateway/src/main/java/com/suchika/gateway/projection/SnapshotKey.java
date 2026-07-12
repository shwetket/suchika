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
     * Phase 1: WEALTH_NET_WORTH_FAMILY has a working compute method.
     * Phase 3: WEALTH_EMI_TRACKING_FAMILY, WEALTH_LIQUIDITY_TIERS_FAMILY,
     * WEALTH_GROWTH_PROJECTION_FAMILY are populated by compute methods.
     * Phase 4 (this phase): WEALTH_FORMULA_GOALS_FAMILY (5-formula goals engine),
     * WEALTH_VALIDATION_REPORT_FAMILY (validation gate with PASS/WARNING/CRITICAL).
     */
    public static final String WEALTH_NET_WORTH_FAMILY = "WEALTH_NET_WORTH_FAMILY";
    public static final String WEALTH_VALIDATION_REPORT_FAMILY = "WEALTH_VALIDATION_REPORT_FAMILY";
    public static final String WEALTH_EMI_TRACKING_FAMILY = "WEALTH_EMI_TRACKING_FAMILY";
    public static final String WEALTH_LIQUIDITY_TIERS_FAMILY = "WEALTH_LIQUIDITY_TIERS_FAMILY";
    public static final String WEALTH_GROWTH_PROJECTION_FAMILY = "WEALTH_GROWTH_PROJECTION_FAMILY";

    /**
     * Epic 8 Phase 4 — five hardcoded formula goals (Debt Crossover, 30-70 Target,
     * Freedom Runway, Insurance Free, Year One). Household-scoped; stored under the
     * admin's own profile_id. Consumes Phase 3 snapshots already in the repository
     * rather than re-calling domain services.
     */
    public static final String WEALTH_FORMULA_GOALS_FAMILY = "WEALTH_FORMULA_GOALS_FAMILY";

    /**
     * ADR-022 Phase 3 — merges each configured {@code wealth.goal_plan} row (objective,
     * target_state, detail, milestones, rules, trigger_events) with its live
     * WEALTH_FORMULA_GOALS_FAMILY entry (matched by goal_id, plus beneficiary_profile_id
     * for YEAR_ONE). Milestone status is derived per-milestone: is_manual_checklist
     * milestones keep their own admin-toggled is_achieved; formula-derived milestones
     * compare the goal's live current_value against the milestone's target_value using
     * the same per-goal-type achieved-direction lookup computeFormulaGoals() uses.
     * INSURANCE_FREE additionally carries the household's active insurance_policy rows
     * as a raw list (no blended total). A goal type with no configured goal_plan row is
     * simply absent from this snapshot — WEALTH_FORMULA_GOALS_FAMILY is unaffected either
     * way. Household-scoped; stored under the admin's own profile_id like every other
     * _FAMILY key.
     */
    public static final String WEALTH_GOAL_DETAIL_FAMILY = "WEALTH_GOAL_DETAIL_FAMILY";

    /**
     * v0.5 Phase 3 — Consolidated Action Center. Household-scoped, stored under
     * the admin's own profile_id like the other _FAMILY keys. Aggregates upcoming
     * calendar events, vehicle compliance deadlines, and biometric streak gaps
     * across all household members into one read-only alert feed.
     */
    public static final String ACTION_CENTER_ALERTS_FAMILY = "ACTION_CENTER_ALERTS_FAMILY";

    private SnapshotKey() {
    }
}
