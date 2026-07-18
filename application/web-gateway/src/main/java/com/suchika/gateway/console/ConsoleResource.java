package com.suchika.gateway.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.suchika.shared.exception.NotFoundException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

/**
 * Admin-only Application Console (Phase 4, ADR-023): live service status,
 * start/stop controls, and an aggregated error feed across all four domains.
 *
 * <p>Gated behind {@code suchika.console.enabled}, defaulting to {@code false}
 * in {@code application.properties} — non-negotiable per the platform-
 * improvements plan (see ADR-023 for why). Every endpoint here 404s when the
 * flag is off, so the feature is fully inert unless explicitly turned on.
 */
@Path("/v1/console")
@Produces(MediaType.APPLICATION_JSON)
public class ConsoleResource {

    @ConfigProperty(name = "suchika.console.enabled", defaultValue = "false")
    boolean consoleEnabled;

    private final ServiceStatusService statusService;
    private final ServiceControlService controlService;
    private final ConsoleErrorAggregationService errorAggregationService;

    public ConsoleResource(
            ServiceStatusService statusService,
            ServiceControlService controlService,
            ConsoleErrorAggregationService errorAggregationService) {
        this.statusService = statusService;
        this.controlService = controlService;
        this.errorAggregationService = errorAggregationService;
    }

    @GET
    @Path("/status")
    public List<ServiceStatus> status() {
        requireEnabled();
        return statusService.listStatuses();
    }

    @POST
    @Path("/services/{name}/start")
    public ServiceActionResult startService(@PathParam("name") String name) {
        requireEnabled();
        return controlService.start(name);
    }

    @POST
    @Path("/services/{name}/stop")
    public ServiceActionResult stopService(@PathParam("name") String name) {
        requireEnabled();
        return controlService.stop(name);
    }

    @GET
    @Path("/errors")
    public JsonNode errors(@QueryParam("since") String since, @QueryParam("limit") Integer limit) {
        requireEnabled();
        return errorAggregationService.aggregate(since, limit);
    }

    private void requireEnabled() {
        if (!consoleEnabled) {
            throw new NotFoundException("Application Console is disabled (suchika.console.enabled=false)");
        }
    }
}
