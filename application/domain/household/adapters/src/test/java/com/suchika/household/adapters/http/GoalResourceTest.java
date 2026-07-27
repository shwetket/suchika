package com.suchika.household.adapters.http;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.input.GoalUseCase;
import com.suchika.household.ports.input.PagedGoals;
import com.suchika.shared.exception.NotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
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
class GoalResourceTest {

    @InjectMock
    GoalUseCase goalUseCase;

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GOAL_ID    = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private Goal sampleGoal;

    @BeforeEach
    void setUp() {
        sampleGoal = Goal.builder()
                .id(GOAL_ID)
                .profileId(PROFILE_ID)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("100000.00"))
                .currentAmount(new BigDecimal("30000.00"))
                .monthlySaving(new BigDecimal("5000.00"))
                .targetDate(LocalDate.of(2027, Month.DECEMBER, 31))
                .status(GoalStatus.ACTIVE)
                .notes("Safety net")
                .createdAt(Instant.EPOCH)
                .build();
    }

    @Test
    void listGoals_returns200WithGoals() {
        Mockito.when(goalUseCase.listPaginated(eq(PROFILE_ID), isNull(), eq(0), eq(50)))
                .thenReturn(new PagedGoals(List.of(sampleGoal), 1));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/goals")
                .then()
                .statusCode(200)
                .body("goals", hasSize(1))
                .body("goals[0].id", is(GOAL_ID.toString()))
                .body("goals[0].goal_name", is("Emergency Fund"))
                .body("goals[0].status", is("ACTIVE"))
                .body("total_size", is(1))
                .body("page", is(0))
                .body("size", is(50));
    }

    @Test
    void listGoals_emptyResult_returns200WithEmptyList() {
        Mockito.when(goalUseCase.listPaginated(eq(PROFILE_ID), isNull(), eq(0), eq(50)))
                .thenReturn(new PagedGoals(List.of(), 0));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/goals")
                .then()
                .statusCode(200)
                .body("goals", hasSize(0))
                .body("total_size", is(0));
    }

    @Test
    void listGoals_missingProfileId_returns400() {
        given()
                .when().get("/v1/goals")
                .then()
                .statusCode(400);
    }

    // ---- Q54 pagination pass ----

    @Test
    void listGoals_honorsExplicitPageAndSize() {
        Mockito.when(goalUseCase.listPaginated(eq(PROFILE_ID), isNull(), eq(3), eq(20)))
                .thenReturn(new PagedGoals(List.of(sampleGoal), 65));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("page", 3)
                .queryParam("size", 20)
                .when().get("/v1/goals")
                .then()
                .statusCode(200)
                .body("total_size", is(65))
                .body("page", is(3))
                .body("size", is(20));
    }

    @Test
    void listGoals_negativePage_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("page", -1)
                .when().get("/v1/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void listGoals_sizeZero_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("size", 0)
                .when().get("/v1/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void listGoals_sizeExceedsMax_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("size", 500)
                .when().get("/v1/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void createGoal_validRequest_returns201() {
        Mockito.when(goalUseCase.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleGoal);

        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"goal_name\":\"Emergency Fund\","
                + "\"target_amount\":100000.00,"
                + "\"monthly_saving\":5000.00,"
                + "\"target_date\":\"2027-12-31\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/goals")
                .then()
                .statusCode(201)
                .body("id", is(GOAL_ID.toString()))
                .body("goal_name", is("Emergency Fund"))
                .body("status", is("ACTIVE"));
    }

    @Test
    void createGoal_missingGoalName_returns400() {
        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"target_amount\":100000.00"
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void createGoal_missingProfileId_returns400() {
        String body = "{"
                + "\"goal_name\":\"Emergency Fund\","
                + "\"target_amount\":100000.00"
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/goals")
                .then()
                .statusCode(400);
    }

    @Test
    void getGoal_exists_returns200() {
        Mockito.when(goalUseCase.get(GOAL_ID))
                .thenReturn(sampleGoal);

        given()
                .when().get("/v1/goals/" + GOAL_ID)
                .then()
                .statusCode(200)
                .body("id", is(GOAL_ID.toString()))
                .body("goal_name", is("Emergency Fund"));
    }

    @Test
    void getGoal_notFound_returns404() {
        Mockito.when(goalUseCase.get(GOAL_ID))
                .thenThrow(new NotFoundException("goal not found"));

        given()
                .when().get("/v1/goals/" + GOAL_ID)
                .then()
                .statusCode(404);
    }

    @Test
    void updateGoal_returns200() {
        Goal updatedGoal = Goal.builder()
                .id(GOAL_ID)
                .profileId(PROFILE_ID)
                .goalName("Emergency Fund Updated")
                .targetAmount(new BigDecimal("120000.00"))
                .currentAmount(new BigDecimal("30000.00"))
                .status(GoalStatus.ACTIVE)
                .createdAt(Instant.EPOCH)
                .build();

        Mockito.when(goalUseCase.update(eq(GOAL_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(updatedGoal);

        String body = "{"
                + "\"goal_name\":\"Emergency Fund Updated\","
                + "\"target_amount\":120000.00,"
                + "\"status\":\"ACTIVE\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().patch("/v1/goals/" + GOAL_ID)
                .then()
                .statusCode(200)
                .body("id", is(GOAL_ID.toString()))
                .body("goal_name", is("Emergency Fund Updated"));
    }

    @Test
    void deleteGoal_returns204() {
        Mockito.doNothing().when(goalUseCase).delete(GOAL_ID);

        given()
                .when().delete("/v1/goals/" + GOAL_ID)
                .then()
                .statusCode(204);
    }

    @Test
    void updateGoalCurrentAmount_returns200() {
        Goal updatedGoal = Goal.builder()
                .id(GOAL_ID)
                .profileId(PROFILE_ID)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("100000.00"))
                .currentAmount(new BigDecimal("50000.00"))
                .status(GoalStatus.ACTIVE)
                .createdAt(Instant.EPOCH)
                .build();

        Mockito.when(goalUseCase.updateCurrentAmount(eq(GOAL_ID), any()))
                .thenReturn(updatedGoal);

        String body = "{\"current_amount\":50000.00}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/v1/goals/" + GOAL_ID + "/current-amount")
                .then()
                .statusCode(200)
                .body("id", is(GOAL_ID.toString()))
                .body("goal_name", notNullValue());
    }

    @Test
    void updateGoalCurrentAmount_missingAmount_returns400() {
        String body = "{}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/v1/goals/" + GOAL_ID + "/current-amount")
                .then()
                .statusCode(400);
    }
}
