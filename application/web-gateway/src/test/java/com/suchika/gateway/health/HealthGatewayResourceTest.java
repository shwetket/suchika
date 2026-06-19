package com.suchika.gateway.health;

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
class HealthGatewayResourceTest {

    @InjectMock
    @RestClient
    HealthServiceClient healthServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        when(healthServiceClient.getVital(UUID.fromString("f3b90000-0000-0000-0000-000000000001")))
                .thenReturn(mapper.readTree(
                        "{\"id\":\"f3b90000-0000-0000-0000-000000000001\","
                        + "\"vital_type\":\"BLOOD_PRESSURE\","
                        + "\"value_primary\":120.0,"
                        + "\"value_secondary\":80.0}"));

        Response mockRecord = mock(Response.class);
        when(mockRecord.getStatus()).thenReturn(201);
        when(mockRecord.readEntity(String.class))
                .thenReturn("{\"id\":\"bbb00000-0000-0000-0000-000000000000\"}");
        when(healthServiceClient.recordVital(any())).thenReturn(mockRecord);
    }

    @Test
    void testGetSeededVital() {
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
