package com.suchika.gateway.wealth;

import com.suchika.gateway.DbSeeder;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class WealthGatewayResourceTest {

    @BeforeAll
    static void init() {
        DbSeeder.seed();
    }

    @Test
    void testGetSeededAccount() {
        // Assert on the seeded account from R__seed_wealth_test_data.sql
        given()
                .when()
                .get("/v1/accounts/f3b90000-0000-0000-0000-000000000000")
                .then()
                .statusCode(200)
                .body("account_id", is("f3b90000-0000-0000-0000-000000000000"))
                .body("account_name", is("Test Account"))
                .body("institution_name", is("Test Bank"));
    }

    @Test
    void testCreateAccount() {
        String uniqueName = "E2E Account - " + UUID.randomUUID();
        String accountJson = "{"
                + "\"account_name\":\"" + uniqueName + "\","
                + "\"account_type\":\"SAVINGS\","
                + "\"institution_name\":\"E2E Bank\","
                + "\"opening_balance\":5000.00"
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(accountJson)
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(201)
                .body("account_id", notNullValue())
                .body("account_name", is(uniqueName));
    }
}
