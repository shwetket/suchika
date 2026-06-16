package com.suchika.gateway.wealth;

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
class WealthGatewayResourceTest {

    @InjectMock
    @RestClient
    WealthServiceClient wealthServiceClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testListAccounts() throws Exception {
        JsonNode mockResponse = mapper.readTree("[{\"id\":\"f3b90000-0000-0000-0000-000000000000\",\"account_name\":\"Test Account\"}]");
        Mockito.when(wealthServiceClient.listAccounts("SAVINGS", true)).thenReturn(mockResponse);

        given()
                .queryParam("account_type", "SAVINGS")
                .queryParam("is_active", true)
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("[0].account_name", is("Test Account"));
    }

    @Test
    void testCreateAccount() {
        Response mockResponse = Mockito.mock(Response.class);
        Mockito.when(mockResponse.getStatus()).thenReturn(201);
        Mockito.when(mockResponse.readEntity(String.class)).thenReturn("{\"id\":\"f3b90000-0000-0000-0000-000000000000\"}");
        
        Mockito.when(wealthServiceClient.createAccount(Mockito.any())).thenReturn(mockResponse);

        given()
                .contentType(ContentType.JSON)
                .body("{\"account_name\":\"New Account\"}")
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(201)
                .body("id", is("f3b90000-0000-0000-0000-000000000000"));
    }
}
