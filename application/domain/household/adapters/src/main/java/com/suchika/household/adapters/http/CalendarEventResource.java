package com.suchika.household.adapters.http;

import com.suchika.household.adapters.http.dto.CalendarEventDto;
import com.suchika.household.adapters.http.dto.CalendarEventResponse;
import com.suchika.household.adapters.http.dto.CreateCalendarEventRequest;
import com.suchika.household.adapters.http.dto.ListCalendarEventsResponse;
import com.suchika.household.adapters.http.dto.UpdateCalendarEventRequest;
import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.input.CalendarEventUseCase;
import com.suchika.household.ports.input.PagedCalendarEvents;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/v1/calendar-events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CalendarEventResource {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final CalendarEventUseCase useCase;

    public CalendarEventResource(CalendarEventUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public ListCalendarEventsResponse list(
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("event_type") String eventType,
            @QueryParam("from_date") LocalDate fromDate,
            @QueryParam("to_date") LocalDate toDate,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {
        if (profileId == null) throw new BadRequestException("profile_id is required");
        EventType type = parseEventType(eventType);
        int page = parsePage(pageParam);
        int size = parseSize(sizeParam);

        PagedCalendarEvents result = useCase.listPaginated(profileId, type, fromDate, toDate, page, size);
        List<CalendarEventDto> events = result.events().stream().map(CalendarEventDto::from).toList();
        return new ListCalendarEventsResponse(events, result.totalCount(), page, size);
    }

    @POST
    public Response create(CreateCalendarEventRequest request) {
        if (request.profileId == null) throw new BadRequestException("profile_id is required");
        if (request.title == null || request.title.isBlank()) throw new BadRequestException("title is required");
        if (request.eventType == null) throw new BadRequestException("event_type is required");
        if (request.startDate == null) throw new BadRequestException("start_date is required");

        EventType eventType = parseEventType(request.eventType);
        CalendarEvent created = useCase.create(request.profileId, request.title, eventType,
                request.startDate, request.endDate, request.location, request.notes);

        List<CalendarEvent> conflicts = useCase.findConflicts(
                request.profileId, request.startDate, request.endDate);
        List<CalendarEvent> filteredConflicts = conflicts.stream()
                .filter(c -> !c.getId().equals(created.getId()))
                .toList();

        CalendarEventResponse response = CalendarEventResponse.from(created, filteredConflicts);
        return Response.status(201).entity(response).build();
    }

    @GET
    @Path("/{id}")
    public CalendarEventDto get(@PathParam("id") UUID id) {
        return CalendarEventDto.from(useCase.get(id));
    }

    @PATCH
    @Path("/{id}")
    public CalendarEventDto update(@PathParam("id") UUID id, UpdateCalendarEventRequest request) {
        EventType eventType = parseEventType(request.eventType);
        return CalendarEventDto.from(useCase.update(id, request.title, eventType,
                request.startDate, request.endDate, request.location, request.notes));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        useCase.delete(id);
        return Response.noContent().build();
    }

    private EventType parseEventType(String value) {
        if (value == null) return null;
        try {
            return EventType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid event_type: " + value);
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
