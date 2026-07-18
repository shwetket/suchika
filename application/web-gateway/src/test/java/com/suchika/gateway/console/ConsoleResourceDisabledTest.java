package com.suchika.gateway.console;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Confirms the Application Console is fully inert under the DEFAULT config
 * (no {@code @TestProfile} override here, unlike {@link ConsoleResourceTest})
 * -- {@code suchika.console.enabled=false} in application.properties is
 * non-negotiable per the platform-improvements plan (ADR-023): every endpoint
 * must 404, not just refuse to do anything useful.
 */
@QuarkusTest
class ConsoleResourceDisabledTest {

    @Test
    void status_disabledByDefault_returns404() {
        given()
                .when().get("/v1/console/status")
                .then()
                .statusCode(404);
    }

    @Test
    void startService_disabledByDefault_returns404() {
        given()
                .when().post("/v1/console/services/wealth/start")
                .then()
                .statusCode(404);
    }

    @Test
    void errors_disabledByDefault_returns404() {
        given()
                .when().get("/v1/console/errors")
                .then()
                .statusCode(404);
    }
}
