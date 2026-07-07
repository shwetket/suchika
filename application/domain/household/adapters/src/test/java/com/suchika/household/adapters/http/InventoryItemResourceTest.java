package com.suchika.household.adapters.http;

import com.suchika.household.domain.InventoryItem;
import com.suchika.household.domain.ItemUnit;
import com.suchika.household.domain.SourcePlatform;
import com.suchika.household.ports.input.InventoryItemUseCase;
import com.suchika.household.ports.input.PagedInventoryItems;
import com.suchika.household.ports.input.UpdateInventoryItemCommand;
import com.suchika.shared.exception.NotFoundException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@QuarkusTest
class InventoryItemResourceTest {

    @InjectMock
    InventoryItemUseCase inventoryItemUseCase;

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ITEM_ID    = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private InventoryItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = InventoryItem.builder()
                .id(ITEM_ID)
                .profileId(PROFILE_ID)
                .itemName("Basmati Rice")
                .quantity(new BigDecimal("5.0"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.BLINKIT)
                .purchaseDate(LocalDate.of(2026, Month.JUNE, 1))
                .category("Grains")
                .createdAt(Instant.EPOCH)
                .build();
    }

    @Test
    void listInventoryItems_returns200WithItems() {
        Mockito.when(inventoryItemUseCase.listPaginated(eq(PROFILE_ID), isNull(), isNull(), eq(0), eq(50)))
                .thenReturn(new PagedInventoryItems(List.of(sampleItem), 1));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(200)
                .body("inventory_items", hasSize(1))
                .body("inventory_items[0].id", is(ITEM_ID.toString()))
                .body("inventory_items[0].item_name", is("Basmati Rice"))
                .body("inventory_items[0].unit", is("KG"))
                .body("total_size", is(1))
                .body("page", is(0))
                .body("size", is(50));
    }

    @Test
    void listInventoryItems_emptyResult_returns200WithEmptyList() {
        Mockito.when(inventoryItemUseCase.listPaginated(eq(PROFILE_ID), isNull(), isNull(), eq(0), eq(50)))
                .thenReturn(new PagedInventoryItems(List.of(), 0));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(200)
                .body("inventory_items", hasSize(0))
                .body("total_size", is(0));
    }

    @Test
    void listInventoryItems_missingProfileId_returns400() {
        given()
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    // ---- Q54 pagination pass ----

    @Test
    void listInventoryItems_honorsExplicitPageAndSize() {
        Mockito.when(inventoryItemUseCase.listPaginated(eq(PROFILE_ID), isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(new PagedInventoryItems(List.of(sampleItem), 25));

        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(200)
                .body("total_size", is(25))
                .body("page", is(2))
                .body("size", is(10));
    }

    @Test
    void listInventoryItems_negativePage_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("page", -1)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    @Test
    void listInventoryItems_sizeZero_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("size", 0)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    @Test
    void listInventoryItems_sizeExceedsMax_returns400() {
        given()
                .queryParam("profile_id", PROFILE_ID)
                .queryParam("size", 500)
                .when().get("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    @Test
    void createInventoryItem_validRequest_returns201() {
        Mockito.when(inventoryItemUseCase.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleItem);

        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"item_name\":\"Basmati Rice\","
                + "\"quantity\":5.0,"
                + "\"unit\":\"KG\","
                + "\"source_platform\":\"BLINKIT\","
                + "\"purchase_date\":\"2026-06-01\","
                + "\"category\":\"Grains\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/inventory-items")
                .then()
                .statusCode(201)
                .body("id", is(ITEM_ID.toString()))
                .body("item_name", is("Basmati Rice"))
                .body("source_platform", is("BLINKIT"));
    }

    @Test
    void createInventoryItem_missingItemName_returns400() {
        String body = "{"
                + "\"profile_id\":\"" + PROFILE_ID + "\","
                + "\"quantity\":5.0,"
                + "\"unit\":\"KG\","
                + "\"source_platform\":\"BLINKIT\","
                + "\"purchase_date\":\"2026-06-01\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    @Test
    void createInventoryItem_missingProfileId_returns400() {
        String body = "{"
                + "\"item_name\":\"Basmati Rice\","
                + "\"quantity\":5.0,"
                + "\"unit\":\"KG\","
                + "\"source_platform\":\"BLINKIT\","
                + "\"purchase_date\":\"2026-06-01\""
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/v1/inventory-items")
                .then()
                .statusCode(400);
    }

    @Test
    void getInventoryItem_exists_returns200() {
        Mockito.when(inventoryItemUseCase.get(ITEM_ID))
                .thenReturn(sampleItem);

        given()
                .when().get("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(200)
                .body("id", is(ITEM_ID.toString()))
                .body("item_name", is("Basmati Rice"));
    }

    @Test
    void getInventoryItem_notFound_returns404() {
        Mockito.when(inventoryItemUseCase.get(ITEM_ID))
                .thenThrow(new NotFoundException("inventory item not found"));

        given()
                .when().get("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(404);
    }

    @Test
    void deleteInventoryItem_returns204() {
        Mockito.doNothing().when(inventoryItemUseCase).delete(ITEM_ID);

        given()
                .when().delete("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(204);
    }

    @Test
    void updateInventoryItem_returns200() {
        InventoryItem updatedItem = InventoryItem.builder()
                .id(ITEM_ID)
                .profileId(PROFILE_ID)
                .itemName("Basmati Rice Premium")
                .quantity(new BigDecimal("6.0"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.BLINKIT)
                .purchaseDate(LocalDate.of(2026, Month.JUNE, 1))
                .category("Grains")
                .createdAt(Instant.EPOCH)
                .build();

        Mockito.when(inventoryItemUseCase.update(eq(ITEM_ID), any(UpdateInventoryItemCommand.class)))
                .thenReturn(updatedItem);

        String body = "{"
                + "\"item_name\":\"Basmati Rice Premium\","
                + "\"quantity\":6.0"
                + "}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(200)
                .body("id", is(ITEM_ID.toString()))
                .body("item_name", is("Basmati Rice Premium"));
    }

    @Test
    void updateInventoryItem_toggleIsConsumed_returns200() {
        InventoryItem consumedItem = InventoryItem.builder()
                .id(ITEM_ID)
                .profileId(PROFILE_ID)
                .itemName("Basmati Rice")
                .quantity(new BigDecimal("5.0"))
                .unit(ItemUnit.KG)
                .sourcePlatform(SourcePlatform.BLINKIT)
                .purchaseDate(LocalDate.of(2026, Month.JUNE, 1))
                .category("Grains")
                .consumed(true)
                .createdAt(Instant.EPOCH)
                .build();

        Mockito.when(inventoryItemUseCase.update(eq(ITEM_ID),
                        argThat(command -> Boolean.TRUE.equals(command.isConsumed()))))
                .thenReturn(consumedItem);

        String body = "{\"is_consumed\":true}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(200)
                .body("id", is(ITEM_ID.toString()))
                .body("is_consumed", is(true));
    }

    @Test
    void updateInventoryItem_notFound_returns404() {
        Mockito.when(inventoryItemUseCase.update(eq(ITEM_ID), any(UpdateInventoryItemCommand.class)))
                .thenThrow(new NotFoundException("inventory item not found"));

        String body = "{\"item_name\":\"Anything\"}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/v1/inventory-items/" + ITEM_ID)
                .then()
                .statusCode(404);
    }
}
