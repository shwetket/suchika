package com.suchika.gateway.health;

import com.suchika.gateway.DbSeeder;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class HealthGatewayResourceTest {

    @BeforeAll
    static void init() {
        DbSeeder.seed();
    }

    @Test
    void testGetSeededVital() {
        // Assert on the seeded vital reading from R__seed_health_test_data.sql
        given()
                .when()
                .get("/v1/vitals/f3b90000-0000-0000-0000-000000000001")
                .then()
                .statusCode(200)
                .body("id", is("f3b90000-0000-0000-0000-000000000001"))
                .body("vital_type", is("BLOOD_PRESSURE"))
                .body("value_primary", is(120.0F))
                .body("value_secondary", is(80.0F));
    }

    @Test
    void testRecordVital() {
        String vitalJson = "{"
                + "\"profile_id\":\"00000000-0000-0000-0000-000000000002\","
                + "\"vital_type\":\"BLOOD_PRESSURE\","
                + "\"reading_date\":\"2026-06-16\","
                + "\"value_primary\":118.0,"
                + "\"value_secondary\":78.0,"
                + "\"unit\":\"mmHg\","
                + "\"notes\":\"E2E Vital Reading\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(vitalJson)
                .when()
                .post("/v1/vitals")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }
}
