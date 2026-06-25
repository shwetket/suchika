package com.suchika.household.adapters.http;

import com.suchika.household.domain.CalendarEvent;
import com.suchika.household.domain.EventType;
import com.suchika.household.ports.input.CalendarEventUseCase;
import com.suchika.shared.exception.NotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@QuarkusTest
class CalendarEventResourceTest {

    @InjectMock
    CalendarEventUseCase calendarEventUseCase;

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CalendarEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = CalendarEvent.builder()
                .id(EVENT_ID)
                .profileId(PROFILE_ID)
                .title("Team Standup")
                .eventType(EventType.WORK)
                .startDate(LocalDate.of(2026, Month.JULY, 1))
                .endDate(LocalDate.of(2026, Month.JULY, 1))
                .location("Office")
                .notes("Daily sync")
                .createdAt(Instant.EPOCH)
                .build();
    }

    @Test
    void listCalendarEvents_emptyResult_returns200WithEmptyList() {
        Mockito.when(calendarEventUseCase.list(eq(PROFILE_ID), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/calendar-events")
                .then()
                .statusCode(200)
                .body("calendar_events", hasSize(0))
                .body("total_size", is(0));
    }

    @Test
    void listCalendarEvents_withOneEvent_returns200WithEvent() {
        Mockito.when(calendarEventUseCase.list(eq(PROFILE_ID), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleEvent));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/calendar-events")
                .then()
                .statusCode(200)
                .body("calendar_events", hasSize(1))
                .body("calendar_events[0].id", is(EVENT_ID.toString()))
                .body("calendar_events[0].title", is("Team Standup"))
                .body("calendar_events[0].event_type", is("WORK"))
                .body("total_size", is(1));
    }

    @Test
    void listCalendarEvents_missingProfileId_returns400() {
        given()
                .when().get("/v1/calendar-events")
                .then()
                .statusCode(400);
    }

    @Test
    void createCalendarEvent_validRequest_returns201WithConflictingEventsEmpty() {
        Mockito.when(calendarEventUseCase.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleEvent);
        Mockito.when(calendarEventUseCase.findConflicts(any(), any(), any()))
                .thenReturn(List.of());

        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"title\":\"Team Standup\","
                + "\"event_type\":\"WORK\","
                + "\"start_date\":\"2026-07-01\","
                + "\"end_date\":\"2026-07-01\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/calendar-events")
                .then()
                .statusCode(201)
                .body("id", is(EVENT_ID.toString()))
                .body("title", is("Team Standup"))
                .body("conflicting_events", hasSize(0));
    }

    @Test
    void createCalendarEvent_missingTitle_returns400() {
        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"event_type\":\"WORK\","
                + "\"start_date\":\"2026-07-01\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/calendar-events")
                .then()
                .statusCode(400);
    }

    @Test
    void createCalendarEvent_missingProfileId_returns400() {
        String body = "{"
                + "\"title\":\"Team Standup\","
                + "\"event_type\":\"WORK\","
                + "\"start_date\":\"2026-07-01\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/calendar-events")
                .then()
                .statusCode(400);
    }

    @Test
    void getCalendarEvent_exists_returns200() {
        Mockito.when(calendarEventUseCase.get(EVENT_ID))
                .thenReturn(sampleEvent);

        given()
                .when().get("/v1/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(200)
                .body("id", is(EVENT_ID.toString()))
                .body("title", is("Team Standup"))
                .body("event_type", is("WORK"));
    }

    @Test
    void getCalendarEvent_notFound_returns404() {
        Mockito.when(calendarEventUseCase.get(EVENT_ID))
                .thenThrow(new NotFoundException("calendar event not found"));

        given()
                .when().get("/v1/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(404);
    }

    @Test
    void updateCalendarEvent_returns200() {
        CalendarEvent updatedEvent = CalendarEvent.builder()
                .id(EVENT_ID)
                .profileId(PROFILE_ID)
                .title("Updated Standup")
                .eventType(EventType.WORK)
                .startDate(LocalDate.of(2026, Month.JULY, 2))
                .createdAt(Instant.EPOCH)
                .build();

        Mockito.when(calendarEventUseCase.update(eq(EVENT_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(updatedEvent);

        String body = "{"
                + "\"title\":\"Updated Standup\","
                + "\"event_type\":\"WORK\","
                + "\"start_date\":\"2026-07-02\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().patch("/v1/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(200)
                .body("id", is(EVENT_ID.toString()))
                .body("title", is("Updated Standup"));
    }

    @Test
    void deleteCalendarEvent_returns204() {
        Mockito.doNothing().when(calendarEventUseCase).delete(EVENT_ID);

        given()
                .when().delete("/v1/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(204);
    }
}
