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

    private SnapshotKey() {
    }
}
