package com.suchika.gateway.projection;

import java.util.List;

/**
 * Response wrapper for dashboard projection endpoints.
 * Snapshots are returned as a flat list; each entry carries its own key and
 * raw JSON payload — the frontend decides how to display each metric type.
 */
public class DashboardResponse {

    private final List<DashboardSnapshotDto> snapshots;

    public DashboardResponse(List<DashboardSnapshotDto> snapshots) {
        this.snapshots = snapshots;
    }

    public List<DashboardSnapshotDto> getSnapshots() {
        return snapshots;
    }
}
