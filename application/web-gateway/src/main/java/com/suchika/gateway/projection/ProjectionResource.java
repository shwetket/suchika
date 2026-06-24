package com.suchika.gateway.projection;

import com.suchika.shared.logging.AppLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/v1/projections")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectionResource {

    private final ProjectionCalculationEngine engine;
    private final DashboardSnapshotRepository snapshotRepository;

    @Inject
    public ProjectionResource(
            ProjectionCalculationEngine engine,
            DashboardSnapshotRepository snapshotRepository) {
        this.engine = engine;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Trigger a full projection refresh for the given profile.
     * Synchronous — returns 200 once all four snapshots are written.
     */
    @POST
    @Path("/refresh/{profileId}")
    public DashboardResponse refresh(@PathParam("profileId") UUID profileId) {
        AppLogger.info("ProjectionResource: refresh triggered for profile %s", profileId);
        return engine.refreshAll(profileId);
    }

    /**
     * Read pre-computed dashboard snapshots for a profile.
     * Instant response — no computation at read time.
     */
    @GET
    @Path("/dashboard/{profileId}")
    public DashboardResponse getDashboard(@PathParam("profileId") UUID profileId) {
        return new DashboardResponse(snapshotRepository.findByProfileId(profileId));
    }
}
