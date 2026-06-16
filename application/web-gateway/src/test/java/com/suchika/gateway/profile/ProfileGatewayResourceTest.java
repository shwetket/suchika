package com.suchika.gateway.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ProfileGatewayResourceTest {

    @InjectMock
    @RestClient
    ProfileServiceClient profileServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testListAdmins() throws Exception {
        JsonNode mockResponse = mapper.readTree("[{\"id\":\"f3b90000-0000-0000-0000-000000000000\",\"email\":\"admin@suchika.com\"}]");
        Mockito.when(profileServiceClient.listAdmins()).thenReturn(mockResponse);

        given()
                .when()
                .get("/v1/admins")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].email", is("admin@suchika.com"));
    }

    @Test
    void testCreateProfile() {
        Response mockResponse = Mockito.mock(Response.class);
        Mockito.when(mockResponse.getStatus()).thenReturn(201);
        Mockito.when(mockResponse.readEntity(String.class)).thenReturn("{\"id\":\"f3b90000-0000-0000-0000-000000000001\"}");

        Mockito.when(profileServiceClient.createProfile(Mockito.any())).thenReturn(mockResponse);

        given()
                .contentType(ContentType.JSON)
                .body("{\"first_name\":\"John\",\"last_name\":\"Doe\",\"role\":\"USER\"}")
                .when()
                .post("/v1/profiles")
                .then()
                .statusCode(201)
                .body("id", is("f3b90000-0000-0000-0000-000000000001"));
    }
}
