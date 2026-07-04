package com.suchika.household.adapters.http;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * True end-to-end integration test: create an inventory item then PUT-update it
 * (exercising the UpdateInventoryItemCommand refactor), through the real HTTP resource,
 * real InventoryItemService, real Panache repository, and real PostgreSQL. Contrast with
 * InventoryItemResourceTest, which @InjectMock's InventoryItemUseCase and never touches the DB.
 * <p>
 * Household's default 'test' profile disables the datasource (like the gateway) so
 * @InjectMock-based resource tests don't need PostgreSQL. This test activates the
 * 'integration-test' profile instead (same pattern as InventoryItemPanacheRepositoryTest)
 * to re-enable the real datasource.
 * <p>
 * Requires a running local PostgreSQL (app_db) with the household + profile schemas migrated.
 * Uses the seeded profile (R__seed_profile_test_data.sql) as the FK target for profile_id,
 * and creates its own InventoryItem rows so it never assumes an empty table or touches
 * the seeded "Basmati Rice" row from R__seed_household_test_data.sql.
 * Runs in a transaction that is rolled back on completion (@TestTransaction).
 */
@QuarkusTest
@TestProfile(InventoryItemCreateUpdateIT.DatabaseIntegrationProfile.class)
@TestTransaction
class InventoryItemCreateUpdateIT {

    /**
     * Activates the 'integration-test' Quarkus config profile which re-enables
     * the datasource. The default 'test' profile disables the DB for @InjectMock tests.
     */
    public static class DatabaseIntegrationProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "integration-test";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of();
        }
    }

    // Seeded in R__seed_profile_test_data.sql — guaranteed to exist when profile service has run
    private static final String SEED_PROFILE_ID = "00000000-0000-0000-0000-000000000002";

    @Test
    void createInventoryItem_thenPutUpdate_persistsChangesInRealDb() {
        String itemId =
                given()
                        .contentType(ContentType.JSON)
                        .body("{"
                                + "\"profile_id\":\"" + SEED_PROFILE_ID + "\","
                                + "\"item_name\":\"IT Wheat Flour\","
                                + "\"quantity\":10.0,"
                                + "\"unit\":\"KG\","
                                + "\"source_platform\":\"BLINKIT\","
                                + "\"purchase_date\":\"2026-07-01\","
                                + "\"category\":\"Grains\""
                                + "}")
                        .when().post("/v1/inventory-items")
                        .then()
                        .statusCode(201)
                        .body("item_name", is("IT Wheat Flour"))
                        .body("id", notNullValue())
                        .extract().path("id");

        // PUT update: only item_name + quantity provided — unit/source_platform/category preserved
        given()
                .contentType(ContentType.JSON)
                .body("{\"item_name\":\"IT Wheat Flour Premium\",\"quantity\":12.5}")
                .when().put("/v1/inventory-items/" + itemId)
                .then()
                .statusCode(200)
                .body("id", is(itemId))
                .body("item_name", is("IT Wheat Flour Premium"))
                .body("quantity", is(12.5f))
                .body("unit", is("KG"))
                .body("source_platform", is("BLINKIT"))
                .body("category", is("Grains"));

        // Round-trip verify via GET, proving the update landed in real Postgres
        given()
                .when().get("/v1/inventory-items/" + itemId)
                .then()
                .statusCode(200)
                .body("item_name", is("IT Wheat Flour Premium"))
                .body("quantity", is(12.5f));
    }

    @Test
    void updateInventoryItem_toggleIsConsumedOnly_leavesOtherFieldsUntouched() {
        String itemId =
                given()
                        .contentType(ContentType.JSON)
                        .body("{"
                                + "\"profile_id\":\"" + SEED_PROFILE_ID + "\","
                                + "\"item_name\":\"IT Toggle Item\","
                                + "\"quantity\":3.0,"
                                + "\"unit\":\"UNITS\","
                                + "\"source_platform\":\"MANUAL\","
                                + "\"purchase_date\":\"2026-07-01\""
                                + "}")
                        .when().post("/v1/inventory-items")
                        .then()
                        .statusCode(201)
                        .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{\"is_consumed\":true}")
                .when().put("/v1/inventory-items/" + itemId)
                .then()
                .statusCode(200)
                .body("is_consumed", is(true))
                .body("item_name", is("IT Toggle Item"))
                .body("quantity", is(3.0f));
    }

    @Test
    void updateInventoryItem_unknownId_returns404() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"item_name\":\"Does Not Exist\"}")
                .when().put("/v1/inventory-items/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}
