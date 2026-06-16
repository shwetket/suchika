package com.suchika.gateway.health;

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
class HealthGatewayResourceTest {

    @InjectMock
    @RestClient
    HealthServiceClient healthServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testListVitals() throws Exception {
        java.util.UUID profileId = java.util.UUID.randomUUID();
        JsonNode mockResponse = mapper.readTree("[{\"id\":\"f3b90000-0000-0000-0000-000000000000\",\"profile_id\":\"" + profileId + "\",\"vital_type\":\"BLOOD_PRESSURE\"}]");
        Mockito.when(healthServiceClient.listVitals(profileId, "BLOOD_PRESSURE")).thenReturn(mockResponse);

        given()
                .queryParam("profile_id", profileId.toString())
                .queryParam("vital_type", "BLOOD_PRESSURE")
                .when()
                .get("/v1/vitals")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].vital_type", is("BLOOD_PRESSURE"));
    }

    @Test
    void testRecordVital() {
        Response mockResponse = Mockito.mock(Response.class);
        Mockito.when(mockResponse.getStatus()).thenReturn(201);
        Mockito.when(mockResponse.readEntity(String.class)).thenReturn("{\"id\":\"f3b90000-0000-0000-0000-000000000001\"}");

        Mockito.when(healthServiceClient.recordVital(Mockito.any())).thenReturn(mockResponse);

        given()
                .contentType(ContentType.JSON)
                .body("{\"profile_id\":\"f3b90000-0000-0000-0000-000000000000\",\"vital_type\":\"BLOOD_PRESSURE\",\"value_systolic\":120,\"value_diastolic\":80}")
                .when()
                .post("/v1/vitals")
                .then()
                .statusCode(201)
                .body("id", is("f3b90000-0000-0000-0000-000000000001"));
    }
}
