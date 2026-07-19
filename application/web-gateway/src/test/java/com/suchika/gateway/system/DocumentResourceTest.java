package com.suchika.gateway.system;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class DocumentResourceTest {

    @Test
    void testGetExistingDocument() {
        // Since tests run from repo root, it will read from the "documents" directory.
        given()
                .when().get("/v1/system/documents/AGENTS.md")
                .then()
                .statusCode(200)
                .body(containsString("Agent"));
    }

    @Test
    void testGetNonExistingDocument() {
        given()
                .when().get("/v1/system/documents/non_existent.md")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetDocumentWithPathTraversal() {
        given()
                .when().get("/v1/system/documents/..%2fAGENTS.md")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetDocumentWithoutExtension() {
        given()
                .when().get("/v1/system/documents/AGENTS")
                .then()
                .statusCode(200)
                .body(containsString("Agent"));
    }
}
