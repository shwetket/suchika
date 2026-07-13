package com.suchika.gateway.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifies ConsoleResource's wiring when the feature is turned on (config
 * override below) -- the default-off behavior is covered separately by
 * {@link ConsoleResourceDisabledTest}, which must NOT enable the flag.
 */
@QuarkusTest
@TestProfile(ConsoleResourceTest.EnabledProfile.class)
class ConsoleResourceTest {

    public static class EnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("suchika.console.enabled", "true");
        }
    }

    @InjectMock
    ServiceStatusService statusService;

    @InjectMock
    ServiceControlService controlService;

    @InjectMock
    ConsoleErrorAggregationService errorAggregationService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(statusService.listStatuses()).thenReturn(List.of(
                new ServiceStatus("profile", 8081, "backend", "UP",
                        new PidRecord(1234L, "java", 8081, "profile", "2026-07-13T00:00:00Z")),
                new ServiceStatus("web", 3000, "frontend", "DOWN", null)));

        when(controlService.start("wealth"))
                .thenReturn(new ServiceActionResult("wealth", "start", "OK", "wealth healthy"));
        when(controlService.stop("wealth"))
                .thenReturn(new ServiceActionResult("wealth", "stop", "OK", "stopped wealth"));
    }

    @Test
    void status_returnsAllServiceStatuses() {
        given()
                .when().get("/v1/console/status")
                .then()
                .statusCode(200)
                .body("[0].name", is("profile"))
                .body("[0].status", is("UP"))
                .body("[0].pid.pid", is(1234))
                .body("[1].name", is("web"))
                .body("[1].status", is("DOWN"));
    }

    @Test
    void startService_delegatesToControlService() {
        given()
                .when().post("/v1/console/services/wealth/start")
                .then()
                .statusCode(200)
                .body("status", is("OK"));
    }

    @Test
    void stopService_delegatesToControlService() {
        given()
                .when().post("/v1/console/services/wealth/stop")
                .then()
                .statusCode(200)
                .body("status", is("OK"));
    }

    @Test
    void errors_delegatesToAggregationService() throws Exception {
        when(errorAggregationService.aggregate(eq("2026-07-01T00:00:00Z"), eq(10)))
                .thenReturn(mapper.readTree("{\"profile\":[],\"wealth\":[],\"health\":[],\"household\":[]}"));

        given()
                .contentType(ContentType.JSON)
                .queryParam("since", "2026-07-01T00:00:00Z")
                .queryParam("limit", 10)
                .when().get("/v1/console/errors")
                .then()
                .statusCode(200)
                .body("profile", is(org.hamcrest.Matchers.empty()));
    }
}
