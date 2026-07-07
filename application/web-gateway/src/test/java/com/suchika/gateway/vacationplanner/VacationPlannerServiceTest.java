package com.suchika.gateway.vacationplanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.gateway.projection.DashboardSnapshotDto;
import com.suchika.gateway.projection.DashboardSnapshotRepository;
import com.suchika.gateway.projection.SnapshotKey;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Plain JUnit 5 unit test for VacationPlannerService — no Quarkus container needed,
 * matching the ProjectionCalculationEngineTest style.
 */
class VacationPlannerServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Mock
    WealthServiceClient wealthServiceClient;

    @Mock
    DashboardSnapshotRepository snapshotRepository;

    private VacationPlannerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VacationPlannerService(wealthServiceClient, snapshotRepository);
        when(wealthServiceClient.listPhysicalAssets(any(), any(), any(), any(), any()))
                .thenReturn(emptyAssets());
    }

    private JsonNode emptyAssets() {
        return parse("{\"physical_assets\":[]}");
    }

    private JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void seedLiquidityTiers(double liquid) {
        when(snapshotRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                new DashboardSnapshotDto(
                        PROFILE_ID,
                        SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY,
                        "{\"tiers\":{\"LIQUID\":" + liquid + "},\"total\":" + liquid + "}",
                        Instant.parse("2026-06-15T00:00:00Z"))));
    }

    @Test
    void checkBudget_throwsWhenProfileIdMissing() {
        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);
        assertThrows(BadRequestException.class, () -> service.checkBudget(null, request));
    }

    @Test
    void checkBudget_throwsWhenTripEndDateMissing() {
        VacationPlannerRequest request = new VacationPlannerRequest();
        assertThrows(BadRequestException.class, () -> service.checkBudget(PROFILE_ID, request));
    }

    @Test
    void checkBudget_returnsUnavailableWhenNoLiquidityTierSnapshotExists() {
        when(snapshotRepository.findByProfileId(PROFILE_ID)).thenReturn(List.of());
        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 50000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        assertEquals("UNAVAILABLE", result.path("budget_check").path("status").asText());
    }

    @Test
    void checkBudget_passesWhenLiquidSavingsCoverTripCost() {
        seedLiquidityTiers(100000);
        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 50000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        JsonNode budgetCheck = result.path("budget_check");
        assertEquals("PASS", budgetCheck.path("status").asText());
        assertEquals(100000.0, budgetCheck.path("liquid_savings").asDouble());
        assertEquals(0.0, budgetCheck.path("shortfall").asDouble());
    }

    @Test
    void checkBudget_warnsWithShortfallWhenLiquidSavingsInsufficient() {
        seedLiquidityTiers(20000);
        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 50000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        JsonNode budgetCheck = result.path("budget_check");
        assertEquals("WARNING", budgetCheck.path("status").asText());
        assertEquals(30000.0, budgetCheck.path("shortfall").asDouble());
    }

    @Test
    void checkBudget_flagsVehicleWithPucExpiringBeforeTripEnd() {
        seedLiquidityTiers(100000);
        when(wealthServiceClient.listPhysicalAssets(any(), any(), any(), any(), any())).thenReturn(parse(
                "{\"physical_assets\":[{\"asset_id\":\"a1\",\"asset_name\":\"Tata Nexon\","
                + "\"metadata\":{\"puc_expiry\":\"2026-08-01\"}}]}"));

        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 1000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        JsonNode compliance = result.path("asset_compliance");
        assertEquals("WARNING", compliance.path("status").asText());
        assertEquals(1, compliance.path("issues").size());
        JsonNode issue = compliance.path("issues").get(0);
        assertEquals("Tata Nexon", issue.path("asset_name").asText());
        assertEquals("PUC_EXPIRED", issue.path("issue_type").asText());
    }

    @Test
    void checkBudget_passesWhenAssetExpiryIsAfterTripEnd() {
        seedLiquidityTiers(100000);
        when(wealthServiceClient.listPhysicalAssets(any(), any(), any(), any(), any())).thenReturn(parse(
                "{\"physical_assets\":[{\"asset_id\":\"a1\",\"asset_name\":\"Tata Nexon\","
                + "\"metadata\":{\"puc_expiry\":\"2027-01-01\",\"insurance_expiry\":\"2027-01-01\"}}]}"));

        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 1000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        assertEquals("PASS", result.path("asset_compliance").path("status").asText());
        assertTrue(result.path("asset_compliance").path("issues").isEmpty());
    }

    @Test
    void checkBudget_ignoresAssetWithUnparseableExpiryDate() {
        seedLiquidityTiers(100000);
        when(wealthServiceClient.listPhysicalAssets(any(), any(), any(), any(), any())).thenReturn(parse(
                "{\"physical_assets\":[{\"asset_id\":\"a1\",\"asset_name\":\"Tata Nexon\","
                + "\"metadata\":{\"puc_expiry\":\"not-a-date\"}}]}"));

        VacationPlannerRequest request = new VacationPlannerRequest();
        request.tripCost = 1000;
        request.tripEndDate = LocalDate.of(2026, Month.AUGUST, 10);

        JsonNode result = service.checkBudget(PROFILE_ID, request);

        assertEquals("PASS", result.path("asset_compliance").path("status").asText());
    }
}
