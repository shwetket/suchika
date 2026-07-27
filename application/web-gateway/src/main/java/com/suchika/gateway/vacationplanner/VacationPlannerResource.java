package com.suchika.gateway.vacationplanner;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/v1/vacation-planner")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VacationPlannerResource {

    private final VacationPlannerService service;

    @Inject
    public VacationPlannerResource(VacationPlannerService service) {
        this.service = service;
    }

    @POST
    @Path("/budget-check")
    public JsonNode budgetCheck(@QueryParam("profile_id") UUID profileId, VacationPlannerRequest request) {
        return service.checkBudget(profileId, request);
    }
}
