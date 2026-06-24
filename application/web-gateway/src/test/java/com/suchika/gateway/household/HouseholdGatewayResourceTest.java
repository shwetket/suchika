package com.suchika.gateway.household;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class HouseholdGatewayResourceTest {

    @InjectMock
    @RestClient
    HouseholdServiceClient householdServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("e0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() throws Exception {
        // list calendar events
        when(householdServiceClient.listCalendarEvents(PROFILE_ID, null, null, null))
                .thenReturn(mapper.readTree(
                        "{\"calendar_events\":[{\"id\":\"" + EVENT_ID + "\","
                        + "\"title\":\"Test Event\","
                        + "\"event_type\":\"PERSONAL\","
                        + "\"start_date\":\"2026-07-01\"}],"
                        + "\"total_size\":1}"));

        // get calendar event
        when(householdServiceClient.getCalendarEvent(EVENT_ID))
                .thenReturn(mapper.readTree(
                        "{\"id\":\"" + EVENT_ID + "\","
                        + "\"title\":\"Test Event\","
                        + "\"event_type\":\"PERSONAL\","
                        + "\"start_date\":\"2026-07-01\"}"));

        // create calendar event — 201
        Response mockCreate = mock(Response.class);
        when(mockCreate.getStatus()).thenReturn(201);
        when(mockCreate.readEntity(String.class))
                .thenReturn("{\"id\":\"" + EVENT_ID + "\",\"title\":\"New Event\"}");
        when(householdServiceClient.createCalendarEvent(any())).thenReturn(mockCreate);

        // list goals
        when(householdServiceClient.listGoals(PROFILE_ID, null))
                .thenReturn(mapper.readTree(
                        "{\"goals\":[{\"id\":\"g1000000-0000-0000-0000-000000000001\","
                        + "\"goal_name\":\"Vacation Fund\","
                        + "\"target_amount\":50000.0,"
                        + "\"current_amount\":10000.0}],"
                        + "\"total_size\":1}"));

        // list inventory items
        when(householdServiceClient.listInventoryItems(PROFILE_ID, null, null))
                .thenReturn(mapper.readTree(
                        "{\"inventory_items\":[],"
                        + "\"total_size\":0}"));
    }

    @Test
    void listCalendarEvents_returns200WithEvents() {
        given()
                .queryParam("profile_id", PROFILE_ID.toString())
                .when()
                .get("/v1/household/calendar-events")
                .then()
                .statusCode(200)
                .body("calendar_events[0].id", is(EVENT_ID.toString()))
                .body("calendar_events[0].title", is("Test Event"))
                .body("total_size", is(1));
    }

    @Test
    void getCalendarEvent_returns200() {
        given()
                .when()
                .get("/v1/household/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(200)
                .body("id", is(EVENT_ID.toString()))
                .body("event_type", is("PERSONAL"));
    }

    @Test
    void createCalendarEvent_returns201() {
        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"title\":\"New Event\","
                + "\"event_type\":\"PERSONAL\","
                + "\"start_date\":\"2026-07-10\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v1/household/calendar-events")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void listGoals_returns200() {
        given()
                .queryParam("profile_id", PROFILE_ID.toString())
                .when()
                .get("/v1/household/goals")
                .then()
                .statusCode(200)
                .body("goals[0].goal_name", is("Vacation Fund"))
                .body("total_size", is(1));
    }

    @Test
    void listInventoryItems_returns200() {
        given()
                .queryParam("profile_id", PROFILE_ID.toString())
                .when()
                .get("/v1/household/inventory-items")
                .then()
                .statusCode(200)
                .body("total_size", is(0));
    }
}
