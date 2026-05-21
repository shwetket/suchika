package com.suchika.gateway.profile;

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
class ProfileGatewayResourceTest {

    @InjectMock
    @RestClient
    ProfileServiceClient profileServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        when(profileServiceClient.listAdmins())
                .thenReturn(mapper.readTree(
                        "{\"admins\":[{\"admin_id\":\"00000000-0000-0000-0000-000000000001\","
                        + "\"email_address\":\"admin@test.com\"}]}"));

        when(profileServiceClient.getProfile(UUID.fromString("00000000-0000-0000-0000-000000000002")))
                .thenReturn(mapper.readTree(
                        "{\"profile_id\":\"00000000-0000-0000-0000-000000000002\","
                        + "\"full_name\":\"Test Member\"}"));

        Response mockCreate = mock(Response.class);
        when(mockCreate.getStatus()).thenReturn(201);
        when(mockCreate.readEntity(String.class))
                .thenReturn("{\"profile_id\":\"11111111-0000-0000-0000-000000000000\","
                        + "\"full_name\":\"E2E Member\"}");
        when(profileServiceClient.createProfile(any())).thenReturn(mockCreate);
    }

    @Test
    void testGetSeededAdminAndProfile() {
        given()
                .when()
                .get("/v1/admins")
                .then()
                .statusCode(200)
                .body("admins.find { it.admin_id == '00000000-0000-0000-0000-000000000001' }.email_address",
                        is("admin@test.com"));

        given()
                .when()
                .get("/v1/profiles/00000000-0000-0000-0000-000000000002")
                .then()
                .statusCode(200)
                .body("profile_id", is("00000000-0000-0000-0000-000000000002"))
                .body("full_name", is("Test Member"));
    }

    @Test
    void testCreateProfile() {
        String profileJson = "{"
                + "\"admin_id\":\"00000000-0000-0000-0000-000000000001\","
                + "\"full_name\":\"E2E Member\","
                + "\"dob\":\"1995-08-25\","
                + "\"relation_to_admin\":\"SIBLING\","
                + "\"email_address\":\"member@test.com\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(profileJson)
                .when()
                .post("/v1/profiles")
                .then()
                .statusCode(201)
                .body("profile_id", notNullValue())
                .body("full_name", is("E2E Member"));
    }
}
