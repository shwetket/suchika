package com.suchika.gateway.vacationplanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.gateway.projection.DashboardSnapshotDto;
import com.suchika.gateway.projection.DashboardSnapshotRepository;
import com.suchika.gateway.projection.SnapshotKey;
import com.suchika.gateway.wealth.WealthServiceClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class VacationPlannerResourceTest {

    @InjectMock
    @RestClient
    WealthServiceClient wealthServiceClient;

    @InjectMock
    DashboardSnapshotRepository snapshotRepository;

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        when(wealthServiceClient.listPhysicalAssets(any(), any(), any()))
                .thenReturn(mapper.readTree("{\"physical_assets\":[]}"));
        when(snapshotRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                new DashboardSnapshotDto(
                        PROFILE_ID,
                        SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY,
                        "{\"tiers\":{\"LIQUID\":100000},\"total\":100000}",
                        Instant.parse("2026-06-15T00:00:00Z"))));
    }

    @Test
    void budgetCheck_returns200WithBudgetAndComplianceSections() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"trip_cost\":50000,\"trip_start_date\":\"2026-08-01\",\"trip_end_date\":\"2026-08-10\"}")
                .when()
                .post("/v1/vacation-planner/budget-check?profile_id=" + PROFILE_ID)
                .then()
                .statusCode(200)
                .body("budget_check.status", is("PASS"))
                .body("asset_compliance.status", is("PASS"));
    }

    @Test
    void budgetCheck_returns400WhenProfileIdMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"trip_cost\":50000,\"trip_end_date\":\"2026-08-10\"}")
                .when()
                .post("/v1/vacation-planner/budget-check")
                .then()
                .statusCode(400);
    }

    @Test
    void budgetCheck_returns400WhenTripEndDateMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"trip_cost\":50000}")
                .when()
                .post("/v1/vacation-planner/budget-check?profile_id=" + PROFILE_ID)
                .then()
                .statusCode(400);
    }
}
