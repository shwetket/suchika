package com.suchika.gateway.profile;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class ProfileGatewayResourceTest {

    @Test
    void testGetSeededAdminAndProfile() {
        // Assert on the seeded admin
        given()
                .when()
                .get("/v1/admins")
                .then()
                .statusCode(200)
                .body("admins.find { it.id == '00000000-0000-0000-0000-000000000001' }.email_address", is("admin@test.com"));

        // Assert on the seeded profile
        given()
                .when()
                .get("/v1/profiles/00000000-0000-0000-0000-000000000002")
                .then()
                .statusCode(200)
                .body("id", is("00000000-0000-0000-0000-000000000002"))
                .body("full_name", is("Test Member"));
    }

    @Test
    void testCreateProfile() {
        String uniqueEmail = "member-" + UUID.randomUUID() + "@test.com";
        String profileJson = "{"
                + "\"admin_id\":\"00000000-0000-0000-0000-000000000001\","
                + "\"full_name\":\"E2E Member\","
                + "\"dob\":\"1995-08-25\","
                + "\"relation_to_admin\":\"SIBLING\","
                + "\"email_address\":\"" + uniqueEmail + "\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(profileJson)
                .when()
                .post("/v1/profiles")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("full_name", is("E2E Member"));
    }
}
