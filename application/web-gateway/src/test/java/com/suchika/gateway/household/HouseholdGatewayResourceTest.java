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
import static org.mockito.Mockito.doNothing;
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

    private static final UUID GOAL_ID     = UUID.fromString("a0000000-0000-0000-0000-000000000003");
    private static final UUID ITEM_ID     = UUID.fromString("b0000000-0000-0000-0000-000000000004");

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

        // update calendar event
        when(householdServiceClient.updateCalendarEvent(any(), any()))
                .thenReturn(mapper.readTree("{\"id\":\"" + EVENT_ID + "\",\"title\":\"Updated\"}"));

        // delete calendar event (void)
        doNothing().when(householdServiceClient).deleteCalendarEvent(any());

        // list goals
        when(householdServiceClient.listGoals(PROFILE_ID, null))
                .thenReturn(mapper.readTree(
                        "{\"goals\":[{\"id\":\"" + GOAL_ID + "\","
                        + "\"goal_name\":\"Vacation Fund\","
                        + "\"target_amount\":50000.0,"
                        + "\"current_amount\":10000.0}],"
                        + "\"total_size\":1}"));

        // create goal — 201
        Response mockGoal = mock(Response.class);
        when(mockGoal.getStatus()).thenReturn(201);
        when(mockGoal.readEntity(String.class))
                .thenReturn("{\"id\":\"" + GOAL_ID + "\",\"goal_name\":\"New Goal\"}");
        when(householdServiceClient.createGoal(any())).thenReturn(mockGoal);

        // get goal
        when(householdServiceClient.getGoal(GOAL_ID))
                .thenReturn(mapper.readTree("{\"id\":\"" + GOAL_ID + "\",\"goal_name\":\"Vacation Fund\"}"));

        // update goal
        when(householdServiceClient.updateGoal(any(), any()))
                .thenReturn(mapper.readTree("{\"id\":\"" + GOAL_ID + "\",\"goal_name\":\"Updated Goal\"}"));

        // delete goal (void)
        doNothing().when(householdServiceClient).deleteGoal(any());

        // list inventory items
        when(householdServiceClient.listInventoryItems(PROFILE_ID, null, null))
                .thenReturn(mapper.readTree(
                        "{\"inventory_items\":[{\"id\":\"" + ITEM_ID + "\","
                        + "\"item_name\":\"Milk\",\"quantity\":2}],"
                        + "\"total_size\":1}"));

        // create inventory item — 201
        Response mockItem = mock(Response.class);
        when(mockItem.getStatus()).thenReturn(201);
        when(mockItem.readEntity(String.class))
                .thenReturn("{\"id\":\"" + ITEM_ID + "\",\"item_name\":\"Milk\"}");
        when(householdServiceClient.createInventoryItem(any())).thenReturn(mockItem);

        // get inventory item
        when(householdServiceClient.getInventoryItem(ITEM_ID))
                .thenReturn(mapper.readTree("{\"id\":\"" + ITEM_ID + "\",\"item_name\":\"Milk\"}"));

        // delete inventory item (void)
        doNothing().when(householdServiceClient).deleteInventoryItem(any());
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
    void updateCalendarEvent_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"title\":\"Updated\"}")
                .when()
                .patch("/v1/household/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(200)
                .body("title", is("Updated"));
    }

    @Test
    void deleteCalendarEvent_returns204() {
        given()
                .when()
                .delete("/v1/household/calendar-events/" + EVENT_ID)
                .then()
                .statusCode(204);
    }

    @Test
    void createGoal_returns201() {
        String body = "{\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"goal_name\":\"New Goal\","
                + "\"target_amount\":50000.0}";
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v1/household/goals")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void getGoal_returns200() {
        given()
                .when()
                .get("/v1/household/goals/" + GOAL_ID)
                .then()
                .statusCode(200)
                .body("goal_name", is("Vacation Fund"));
    }

    @Test
    void updateGoal_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"goal_name\":\"Updated Goal\"}")
                .when()
                .patch("/v1/household/goals/" + GOAL_ID)
                .then()
                .statusCode(200)
                .body("goal_name", is("Updated Goal"));
    }

    @Test
    void deleteGoal_returns204() {
        given()
                .when()
                .delete("/v1/household/goals/" + GOAL_ID)
                .then()
                .statusCode(204);
    }

    @Test
    void listInventoryItems_returns200() {
        given()
                .queryParam("profile_id", PROFILE_ID.toString())
                .when()
                .get("/v1/household/inventory-items")
                .then()
                .statusCode(200)
                .body("total_size", is(1));
    }

    @Test
    void createInventoryItem_returns201() {
        String body = "{\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"item_name\":\"Milk\","
                + "\"quantity\":2}";
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v1/household/inventory-items")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void getInventoryItem_returns200() {
        given()
                .when()
                .get("/v1/household/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(200)
                .body("item_name", is("Milk"));
    }

    @Test
    void deleteInventoryItem_returns204() {
        given()
                .when()
                .delete("/v1/household/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(204);
    }
}
