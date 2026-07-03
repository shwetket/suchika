package com.suchika.gateway.wealth;

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
class WealthGatewayResourceTest {

    @InjectMock
    @RestClient
    WealthServiceClient wealthServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        when(wealthServiceClient.getAccount(UUID.fromString("f3b90000-0000-0000-0000-000000000000")))
                .thenReturn(mapper.readTree(
                        "{\"account_id\":\"f3b90000-0000-0000-0000-000000000000\","
                        + "\"account_name\":\"Test Account\","
                        + "\"institution_name\":\"Test Bank\"}"));

        Response mockCreate = mock(Response.class);
        when(mockCreate.getStatus()).thenReturn(201);
        when(mockCreate.readEntity(String.class))
                .thenReturn("{\"account_id\":\"aaa00000-0000-0000-0000-000000000000\"}");
        when(wealthServiceClient.createAccount(any(), any())).thenReturn(mockCreate);

        when(wealthServiceClient.listTransactions(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mapper.readTree("{\"transactions\":[]}"));

        when(wealthServiceClient.listUploads(any()))
                .thenReturn(mapper.readTree("{\"uploads\":[]}"));
    }

    @Test
    void testGetSeededAccount() {
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
        String accountJson = "{"
                + "\"account_name\":\"E2E Account\","
                + "\"account_type\":\"SAVINGS\","
                + "\"institution_name\":\"E2E Bank\","
                + "\"opening_balance\":5000.00"
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(accountJson)
                .queryParam("profile_id", "00000000-0000-0000-0000-000000000002")
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(201)
                .body("account_id", notNullValue());
    }

    @Test
    void testListTransactionsForSeededAccount() {
        given()
                .when()
                .get("/v1/accounts/f3b90000-0000-0000-0000-000000000000/transactions")
                .then()
                .statusCode(200)
                .body("transactions", notNullValue());
    }

    @Test
    void testListUploadsForSeededAccount() {
        given()
                .when()
                .get("/v1/accounts/f3b90000-0000-0000-0000-000000000000/uploads")
                .then()
                .statusCode(200)
                .body("uploads", notNullValue());
    }
}
