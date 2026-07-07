package com.suchika.household.adapters.http;

import com.suchika.household.adapters.http.dto.CreateGoalRequest;
import com.suchika.household.adapters.http.dto.GoalDto;
import com.suchika.household.adapters.http.dto.ListGoalsResponse;
import com.suchika.household.adapters.http.dto.UpdateGoalCurrentAmountRequest;
import com.suchika.household.adapters.http.dto.UpdateGoalRequest;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.input.GoalUseCase;
import com.suchika.household.ports.input.PagedGoals;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

import java.util.List;
import java.util.UUID;

@Path("/v1/goals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GoalResource {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final GoalUseCase useCase;

    public GoalResource(GoalUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public ListGoalsResponse list(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("status") String status,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {
        if (profileId == null) throw new BadRequestException("profile_id is required");
        GoalStatus goalStatus = parseStatus(status);
        int page = parsePage(pageParam);
        int size = parseSize(sizeParam);

        PagedGoals result = useCase.listPaginated(profileId, goalStatus, page, size);
        List<GoalDto> goals = result.goals().stream().map(GoalDto::from).toList();
        return new ListGoalsResponse(goals, result.totalCount(), page, size);
    }

    @POST
    public Response create(CreateGoalRequest request) {
        if (request.profileId == null) throw new BadRequestException("profile_id is required");
        if (request.goalName == null || request.goalName.isBlank()) throw new BadRequestException("goal_name is required");
        if (request.targetAmount == null) throw new BadRequestException("target_amount is required");

        GoalDto created = GoalDto.from(useCase.create(
                request.profileId, request.goalName, request.targetAmount,
                request.monthlySaving, request.targetDate, request.notes));
        return Response.status(201).entity(created).build();
    }

    @GET
    @Path("/{id}")
    public GoalDto get(@PathParam("id") UUID id) {
        return GoalDto.from(useCase.get(id));
    }

    @PATCH
    @Path("/{id}")
    public GoalDto update(@PathParam("id") UUID id, UpdateGoalRequest request) {
        GoalStatus goalStatus = parseStatus(request.status);
        return GoalDto.from(useCase.update(id, request.goalName, request.targetAmount,
                request.monthlySaving, request.targetDate, goalStatus, request.notes));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/current-amount")
    public GoalDto updateCurrentAmount(@PathParam("id") UUID id, UpdateGoalCurrentAmountRequest request) {
        if (request.currentAmount == null) throw new BadRequestException("current_amount is required");
        return GoalDto.from(useCase.updateCurrentAmount(id, request.currentAmount));
    }

    private GoalStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return GoalStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + value);
        }
    }

    private int parsePage(Integer pageParam) {
        int page = pageParam != null ? pageParam : 0;
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        return page;
    }

    private int parseSize(Integer sizeParam) {
        int size = sizeParam != null ? sizeParam : DEFAULT_PAGE_SIZE;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }
}
