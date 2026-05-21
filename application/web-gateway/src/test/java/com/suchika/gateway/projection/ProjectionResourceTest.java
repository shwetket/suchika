package com.suchika.gateway.projection;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProjectionResourceTest {

    @InjectMock
    ProjectionCalculationEngine engine;

    @InjectMock
    DashboardSnapshotRepository snapshotRepository;

    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant TEST_INSTANT = LocalDate.of(2026, Month.JUNE, 25).atStartOfDay().toInstant(ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        List<DashboardSnapshotDto> snapshots = List.of(
                new DashboardSnapshotDto(
                        PROFILE_ID,
                        SnapshotKey.WEALTH_NET_WORTH,
                        "{\"net_worth\":100000.0,\"account_count\":2}",
                        TEST_INSTANT),
                new DashboardSnapshotDto(
                        PROFILE_ID,
                        SnapshotKey.HEALTH_VITALS_SUMMARY,
                        "{\"vitals\":[]}",
                        TEST_INSTANT));

        when(engine.refreshAll(PROFILE_ID)).thenReturn(new DashboardResponse(snapshots));
        when(snapshotRepository.findByProfileId(PROFILE_ID)).thenReturn(snapshots);
    }

    @Test
    void refresh_returns200WithSnapshots() {
        given()
                .contentType(io.restassured.http.ContentType.JSON)
                .when()
                .post("/v1/projections/refresh/" + PROFILE_ID)
                .then()
                .statusCode(200)
                .body("snapshots", notNullValue())
                .body("snapshots.size()", is(2))
                .body("snapshots[0].snapshot_key", is(SnapshotKey.WEALTH_NET_WORTH));

        verify(engine).refreshAll(PROFILE_ID);
    }

    @Test
    void getDashboard_returns200WithSnapshots() {
        given()
                .when()
                .get("/v1/projections/dashboard/" + PROFILE_ID)
                .then()
                .statusCode(200)
                .body("snapshots", notNullValue())
                .body("snapshots.size()", is(2))
                .body("snapshots[1].snapshot_key", is(SnapshotKey.HEALTH_VITALS_SUMMARY));

        verify(snapshotRepository).findByProfileId(PROFILE_ID);
    }

    @Test
    void getDashboard_returnsEmptyListWhenNoRefreshRun() {
        when(snapshotRepository.findByProfileId(any())).thenReturn(List.of());

        given()
                .when()
                .get("/v1/projections/dashboard/" + UUID.randomUUID())
                .then()
                .statusCode(200)
                .body("snapshots.size()", is(0));
    }
}
