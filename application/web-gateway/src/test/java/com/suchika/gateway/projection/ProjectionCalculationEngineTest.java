package com.suchika.gateway.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.profile.ProfileServiceClient;
import com.suchika.gateway.wealth.WealthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain JUnit 5 unit test for ProjectionCalculationEngine.
 * All dependencies are mocked manually — no Quarkus container needed.
 */
class ProjectionCalculationEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, Month.JUNE, 15);

    @Mock
    WealthServiceClient wealthClient;

    @Mock
    HealthServiceClient healthClient;

    @Mock
    HouseholdServiceClient householdClient;

    @Mock
    ProfileServiceClient profileClient;

    @Mock
    DashboardSnapshotRepository snapshotRepo;

    ProjectionCalculationEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new ProjectionCalculationEngine(wealthClient, healthClient, householdClient, profileClient, snapshotRepo);
        when(wealthClient.listAccounts(isNull(), eq(true), anyString())).thenReturn(buildEmptyAccountsResponse());
        when(wealthClient.listPhysicalAssets(isNull(), eq(true), anyString(), isNull(), isNull()))
                .thenReturn(MAPPER.createObjectNode().set("physical_assets", MAPPER.createArrayNode()));
        when(householdClient.listGoals(any(), isNull(), isNull(), isNull())).thenReturn(MAPPER.createObjectNode().set("goals", MAPPER.createArrayNode()));
        when(wealthClient.listGoalPlans(any())).thenReturn(MAPPER.createObjectNode().set("goal_plans", MAPPER.createArrayNode()));
        // ADR-022 THIRTY_SEVENTY_TARGET runs on every computeFormulaGoals call and pages
        // through every member's every account's transactions — default to empty so tests
        // that don't care about this goal specifically don't need their own stub.
        when(wealthClient.listTransactions(any(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(MAPPER.createObjectNode().set("transactions", MAPPER.createArrayNode()));
        // ADR-022 Phase 2 — THIRTY_SEVENTY_TARGET also sums active insurance policy
        // premiums; default to empty so tests that don't care about this term don't
        // need their own stub.
        when(wealthClient.listInsurancePolicies(any()))
                .thenReturn(MAPPER.createObjectNode().set("insurance_policies", MAPPER.createArrayNode()));
    }

    // ── computeNetWorth ───────────────────────────────────────────────────────

    @Test
    void computeNetWorth_sumsAccountBalances() throws Exception {
        UUID accountId1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
        UUID accountId2 = UUID.fromString("11111111-0000-0000-0000-000000000002");
        JsonNode accountsResponse = buildAccountsResponse(accountId1, accountId2);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
        stubBalance(accountId1, 1000.0);
        stubBalance(accountId2, 500.0);

        engine.computeNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(1500.0, payload.path("net_worth").asDouble(), 0.001);
        assertEquals(2, payload.path("account_count").asInt());
    }

    @Test
    void computeNetWorth_emptyAccounts_storesZero() throws Exception {
        JsonNode emptyResponse = buildEmptyAccountsResponse();
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(emptyResponse);

        engine.computeNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0.0, payload.path("net_worth").asDouble(), 0.001);
        assertEquals(0, payload.path("account_count").asInt());
    }

    @Test
    void computeNetWorth_usesCurrentBalanceEndpoint_notOpeningBalance() throws Exception {
        // Epic 8 Phase 1, Bug 2 fix: net worth must reflect opening_balance + SUM(CREDIT) - SUM(DEBIT),
        // not just opening_balance. This test proves the engine calls the per-account balance endpoint
        // and uses current_balance, ignoring whatever opening_balance happens to be on the list payload.
        UUID accountId = UUID.fromString("22222222-0000-0000-0000-000000000001");
        ObjectNode account = MAPPER.createObjectNode();
        account.put("account_id", accountId.toString());
        account.put("opening_balance", 100.0); // deliberately wrong/stale if engine read this directly
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode accounts = MAPPER.createArrayNode();
        accounts.add(account);
        root.set("accounts", accounts);

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString()))).thenReturn(root);
        stubBalance(accountId, 9999.0);

        engine.computeNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), payloadCaptor.capture());
        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());

        assertEquals(9999.0, payload.path("net_worth").asDouble(), 0.001);
        verify(wealthClient).getAccountBalance(accountId, PROFILE_ID.toString());
    }

    // ── computeNetWorth: physical asset value (v1.0 net-worth-model gap) ────────

    @Test
    void computeNetWorth_includesActivePhysicalAssetValue() throws Exception {
        UUID accountId = UUID.fromString("11111111-0000-0000-0000-000000000009");
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        stubBalance(accountId, 1000.0);
        when(wealthClient.listPhysicalAssets(isNull(), eq(true), eq(PROFILE_ID.toString()), isNull(), isNull()))
                .thenReturn(buildPhysicalAssetsWithValueResponse(9000000.0));

        engine.computeNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), payloadCaptor.capture());
        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());

        assertEquals(9001000.0, payload.path("net_worth").asDouble(), 0.001);
        assertEquals(9000000.0, payload.path("physical_asset_value").asDouble(), 0.001);
    }

    @Test
    void computeNetWorth_excludesInactivePhysicalAssets() throws Exception {
        // The engine requests only active assets (is_active=true) — an inactive asset's
        // current_value is filtered out server-side and never reaches this summation.
        UUID accountId = UUID.fromString("11111111-0000-0000-0000-00000000000a");
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        stubBalance(accountId, 1000.0);
        when(wealthClient.listPhysicalAssets(isNull(), eq(true), eq(PROFILE_ID.toString()), isNull(), isNull()))
                .thenReturn(MAPPER.createObjectNode().set("physical_assets", MAPPER.createArrayNode()));

        engine.computeNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), payloadCaptor.capture());
        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());

        assertEquals(1000.0, payload.path("net_worth").asDouble(), 0.001);
        assertEquals(0.0, payload.path("physical_asset_value").asDouble(), 0.001);
        verify(wealthClient).listPhysicalAssets(isNull(), eq(true), eq(PROFILE_ID.toString()), isNull(), isNull());
    }

    @Test
    void computeGoalProgress_ignoresPhysicalAssetValue_regressionGuardForLiquidityScopeBoundary() throws Exception {
        // Deliberate scope boundary: physical asset value must count toward WEALTH_NET_WORTH
        // but NOT toward computeTotalBalance/goal-progress math (illiquid vs. liquid capital).
        // Stub a huge physical asset value for this profile — if computeTotalBalance ever
        // started summing it in, this goal's progress_percent would jump well past what the
        // account balance alone justifies.
        UUID goalId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
        JsonNode goalsResponse = buildGoalsResponse(goalId, "Vacation Fund", 1000.0);
        UUID accountId = UUID.fromString("11111111-0000-0000-0000-00000000000b");
        JsonNode accountsResponse = buildAccountsResponse(accountId);

        when(householdClient.listGoals(PROFILE_ID, null, null, null)).thenReturn(goalsResponse);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
        stubBalance(accountId, 600.0);
        when(householdClient.updateGoalCurrentAmount(eq(goalId), any()))
                .thenReturn(MAPPER.createObjectNode());
        when(wealthClient.listPhysicalAssets(isNull(), eq(true), eq(PROFILE_ID.toString()), isNull(), isNull()))
                .thenReturn(buildPhysicalAssetsWithValueResponse(9000000.0));

        engine.computeGoalProgress(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), payloadCaptor.capture());
        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());

        // Unchanged from the pre-existing 60% (600/1000) behavior — no physical asset bleed-through.
        assertEquals(60.0, payload.path("goals").get(0).path("progress_percent").asDouble(), 0.001);
        assertEquals(600.0, payload.path("goals").get(0).path("current_amount").asDouble(), 0.001);
        // computeTotalBalance (used here) never calls listPhysicalAssets at all.
        verify(wealthClient, never()).listPhysicalAssets(any(), any(), any(), any(), any());
    }

    private JsonNode buildPhysicalAssetsWithValueResponse(double currentValue) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode assets = MAPPER.createArrayNode();
        ObjectNode asset = MAPPER.createObjectNode();
        asset.put("asset_id", UUID.randomUUID().toString());
        asset.put("asset_name", "Self-Occupied Flat");
        asset.put("current_value", currentValue);
        assets.add(asset);
        root.set("physical_assets", assets);
        return root;
    }

    // ── computeVitalsSummary ─────────────────────────────────────────────────

    @Test
    void computeVitalsSummary_keepsLatestReadingPerType() throws Exception {
        // Three readings: WEIGHT (newest), WEIGHT (older), HEIGHT — only one WEIGHT should survive
        JsonNode vitalsResponse = buildVitalsResponse(
                new VitalEntry("WEIGHT", 72.0, 0.0, "kg", "2026-06-01"),
                new VitalEntry("WEIGHT", 70.0, 0.0, "kg", "2026-05-01"),
                new VitalEntry("HEIGHT", 175.0, 0.0, "cm", "2026-01-01")
        );
        when(healthClient.listVitals(PROFILE_ID, null, 0, 200)).thenReturn(vitalsResponse);

        engine.computeVitalsSummary(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        JsonNode vitals = payload.path("vitals");
        assertTrue(vitals.isArray());
        // Exactly 2 entries: one WEIGHT (first/newest), one HEIGHT
        assertEquals(2, vitals.size());

        // First entry is WEIGHT with value 72.0 (the newest one)
        assertEquals("WEIGHT", vitals.get(0).path("vital_type").asText());
        assertEquals(72.0, vitals.get(0).path("value_primary").asDouble(), 0.001);
    }

    @Test
    void computeVitalsSummary_emptyVitals_storesEmptyArray() throws Exception {
        JsonNode emptyVitalsResponse = MAPPER.readTree("{\"vital_readings\":[]}");
        when(healthClient.listVitals(PROFILE_ID, null, 0, 200)).thenReturn(emptyVitalsResponse);

        engine.computeVitalsSummary(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0, payload.path("vitals").size());
    }

    // ── computeEventSummary ──────────────────────────────────────────────────

    @Test
    void computeEventSummary_countsUpcomingEvents() throws Exception {
        JsonNode eventsResponse = buildCalendarEventsResponse(3);
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(eventsResponse);

        engine.computeEventSummary(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(3, payload.path("upcoming_count").asInt());
        assertEquals(3, payload.path("events").size());
    }

    @Test
    void computeEventSummary_noEvents_storesZeroCount() throws Exception {
        JsonNode emptyResponse = MAPPER.readTree("{\"calendar_events\":[]}");
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(emptyResponse);

        engine.computeEventSummary(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0, payload.path("upcoming_count").asInt());
    }

    // ── computeGoalProgress ──────────────────────────────────────────────────

    @Test
    void computeGoalProgress_calculatesProgressPercent() throws Exception {
        UUID goalId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        JsonNode goalsResponse = buildGoalsResponse(goalId, "Vacation Fund", 1000.0);
        // Balance = 600 → progress = 60%
        UUID accountId = UUID.fromString("33333333-0000-0000-0000-000000000001");
        JsonNode accountsResponse = buildAccountsResponse(accountId);

        when(householdClient.listGoals(PROFILE_ID, null, null, null)).thenReturn(goalsResponse);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
        stubBalance(accountId, 600.0);
        // stub the updateGoalCurrentAmount call
        when(householdClient.updateGoalCurrentAmount(eq(goalId), any()))
                .thenReturn(MAPPER.createObjectNode());

        engine.computeGoalProgress(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        JsonNode goals = payload.path("goals");
        assertEquals(1, goals.size());
        assertEquals(60.0, goals.get(0).path("progress_percent").asDouble(), 0.001);
        assertEquals(600.0, goals.get(0).path("current_amount").asDouble(), 0.001);

        // Should have called back to household service to update current amount
        verify(householdClient).updateGoalCurrentAmount(eq(goalId), any());
    }

    @Test
    void computeGoalProgress_balanceExceedsTarget_capsAt100Percent() throws Exception {
        UUID goalId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
        JsonNode goalsResponse = buildGoalsResponse(goalId, "Car Fund", 500.0);
        // Balance = 1000 > target 500 → capped at 100%
        UUID accountId = UUID.fromString("44444444-0000-0000-0000-000000000001");
        JsonNode accountsResponse = buildAccountsResponse(accountId);

        when(householdClient.listGoals(PROFILE_ID, null, null, null)).thenReturn(goalsResponse);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
        stubBalance(accountId, 1000.0);
        when(householdClient.updateGoalCurrentAmount(eq(goalId), any()))
                .thenReturn(MAPPER.createObjectNode());

        engine.computeGoalProgress(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        JsonNode goals = payload.path("goals");
        assertEquals(100.0, goals.get(0).path("progress_percent").asDouble(), 0.001);
        // current_amount is capped at target_amount
        assertEquals(500.0, goals.get(0).path("current_amount").asDouble(), 0.001);
    }

    // ── computeCategoryValidation (Epic 8 Phase 1 validation seed, Use Case 8.4 narrow scope) ──

    @Test
    void computeCategoryValidation_noAccountsHaveCategory_flagsAllAsUncategorized() throws Exception {
        // Phase 1: wealth.account.metadata.category is never populated yet (Phase 2 work).
        // This check is EXPECTED to flag every account as uncategorized right now — that is
        // correct, not a bug. It must not be faked to report categorized accounts.
        UUID accountId1 = UUID.fromString("55555555-0000-0000-0000-000000000001");
        UUID accountId2 = UUID.fromString("55555555-0000-0000-0000-000000000002");
        JsonNode accountsResponse = buildAccountsResponse(accountId1, accountId2);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);

        engine.computeCategoryValidation(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(2, payload.path("total_accounts").asInt());
        assertEquals(0, payload.path("categorized_count").asInt());
        assertEquals(2, payload.path("uncategorized_count").asInt());
        JsonNode uncategorizedIds = payload.path("uncategorized_account_ids");
        assertTrue(uncategorizedIds.isArray());
        assertEquals(2, uncategorizedIds.size());
    }

    @Test
    void computeCategoryValidation_accountWithCategorySet_isCountedAsCategorized() throws Exception {
        // Even though Phase 1 doesn't populate category for real accounts, the check itself
        // must correctly recognize a category when one IS present (e.g. set manually via the
        // new classification endpoint) — proving the check logic is honest, not hardcoded.
        UUID categorizedId = UUID.fromString("66666666-0000-0000-0000-000000000001");
        UUID uncategorizedId = UUID.fromString("66666666-0000-0000-0000-000000000002");

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode accounts = MAPPER.createArrayNode();

        ObjectNode withCategory = MAPPER.createObjectNode();
        withCategory.put("account_id", categorizedId.toString());
        ObjectNode metadata = MAPPER.createObjectNode();
        metadata.put("category", "EMERGENCY_FUND");
        withCategory.set("metadata", metadata);
        accounts.add(withCategory);

        ObjectNode withoutCategory = MAPPER.createObjectNode();
        withoutCategory.put("account_id", uncategorizedId.toString());
        accounts.add(withoutCategory);

        root.set("accounts", accounts);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString()))).thenReturn(root);

        engine.computeCategoryValidation(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(2, payload.path("total_accounts").asInt());
        assertEquals(1, payload.path("categorized_count").asInt());
        assertEquals(1, payload.path("uncategorized_count").asInt());
        assertEquals(uncategorizedId.toString(), payload.path("uncategorized_account_ids").get(0).asText());
    }

    @Test
    void computeCategoryValidation_noAccounts_storesZeroes() throws Exception {
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildEmptyAccountsResponse());

        engine.computeCategoryValidation(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0, payload.path("total_accounts").asInt());
        assertEquals(0, payload.path("categorized_count").asInt());
        assertEquals(0, payload.path("uncategorized_count").asInt());
    }

    // ── refreshAll ───────────────────────────────────────────────────────────

    @Test
    void refreshAll_callsAllThirteenComputeMethods() throws Exception {
        // Stub all clients with empty-but-valid responses
        when(wealthClient.listAccounts(any(), any(), any()))
                .thenReturn(buildEmptyAccountsResponse());
        when(wealthClient.listPhysicalAssets(any(), any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"physical_assets\":[]}"));
        when(healthClient.listVitals(any(), any(), anyInt(), anyInt()))
                .thenReturn(MAPPER.readTree("{\"vital_readings\":[]}"));
        when(householdClient.listCalendarEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"calendar_events\":[]}"));
        when(householdClient.listGoals(any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"goals\":[]}"));
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildEmptyProfilesResponse());
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        engine.refreshAll(PROFILE_ID);

        // All thirteen SnapshotKey upserts must have fired (6 original + 3 Phase 3 +
        // 2 Phase 4 + 1 Phase 3 v0.5 + 1 ADR-022 Phase 3)
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_VALIDATION_REPORT_FAMILY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), anyString());
        // Total: exactly 13 upsert calls
        verify(snapshotRepo, times(13)).upsert(any(), anyString(), anyString());
    }

    @Test
    void refreshAll_oneStepThrows_othersStillRun() throws Exception {
        // Bug 3 fix: a RuntimeException in one compute step (here, wealth-dependent
        // steps fail because listAccounts throws) must not block independent steps
        // (health, household) from refreshing and upserting their own snapshots.
        when(wealthClient.listAccounts(any(), any(), any()))
                .thenThrow(new RuntimeException("wealth service unavailable"));
        when(healthClient.listVitals(any(), any(), anyInt(), anyInt()))
                .thenReturn(MAPPER.readTree("{\"vital_readings\":[]}"));
        when(householdClient.listCalendarEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"calendar_events\":[]}"));
        when(householdClient.listGoals(any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"goals\":[]}"));
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildEmptyProfilesResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        assertDoesNotThrow(() -> engine.refreshAll(PROFILE_ID));

        // Health and household summaries still computed despite wealth failures
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), anyString());
        // Wealth-dependent snapshots never upserted for this profile, since their steps threw
        verify(snapshotRepo, never()).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), anyString());
        verify(snapshotRepo, never()).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), anyString());
    }

    // ── computeFamilyNetWorth (ADR-017) ─────────────────────────────────────────

    @Test
    void computeFamilyNetWorth_sumsNetWorthAcrossHouseholdMembers() throws Exception {
        UUID spouseProfileId = UUID.fromString("77777777-0000-0000-0000-000000000002");
        UUID childProfileId = UUID.fromString("77777777-0000-0000-0000-000000000003");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF"),
                new MemberEntry(spouseProfileId, "Shweta", "SPOUSE"),
                new MemberEntry(childProfileId, "Gayan", "CHILD")
        ));

        UUID selfAccountId = UUID.fromString("88888888-0000-0000-0000-000000000001");
        UUID spouseAccountId = UUID.fromString("88888888-0000-0000-0000-000000000002");
        UUID childAccountId = UUID.fromString("88888888-0000-0000-0000-000000000003");

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(selfAccountId));
        when(wealthClient.listAccounts(isNull(), eq(true), eq(spouseProfileId.toString())))
                .thenReturn(buildAccountsResponse(spouseAccountId));
        when(wealthClient.listAccounts(isNull(), eq(true), eq(childProfileId.toString())))
                .thenReturn(buildAccountsResponse(childAccountId));

        when(wealthClient.getAccountBalance(selfAccountId, PROFILE_ID.toString()))
                .thenReturn(balanceNode(selfAccountId, 100000.0));
        when(wealthClient.getAccountBalance(spouseAccountId, spouseProfileId.toString()))
                .thenReturn(balanceNode(spouseAccountId, 50000.0));
        when(wealthClient.getAccountBalance(childAccountId, childProfileId.toString()))
                .thenReturn(balanceNode(childAccountId, 5000.0));

        engine.computeFamilyNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(155000.0, payload.path("family_net_worth").asDouble(), 0.001);
        assertEquals(3, payload.path("member_count").asInt());

        JsonNode members = payload.path("members");
        assertEquals(3, members.size());
        assertEquals("Ketan", members.get(0).path("full_name").asText());
        assertEquals("SELF", members.get(0).path("relation_to_admin").asText());
        assertEquals(100000.0, members.get(0).path("net_worth").asDouble(), 0.001);
        assertEquals("Shweta", members.get(1).path("full_name").asText());
        assertEquals(50000.0, members.get(1).path("net_worth").asDouble(), 0.001);
        assertEquals("Gayan", members.get(2).path("full_name").asText());
        assertEquals(5000.0, members.get(2).path("net_worth").asDouble(), 0.001);
    }

    @Test
    void computeFamilyNetWorth_singleMemberHousehold_equalsOwnNetWorth() throws Exception {
        // Degenerate case: admin is the only active profile in the household.
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")
        ));

        UUID accountId = UUID.fromString("99999999-0000-0000-0000-000000000001");
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.getAccountBalance(accountId, PROFILE_ID.toString()))
                .thenReturn(balanceNode(accountId, 42000.0));

        engine.computeFamilyNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(42000.0, payload.path("family_net_worth").asDouble(), 0.001);
        assertEquals(1, payload.path("member_count").asInt());
    }

    @Test
    void computeFamilyNetWorth_noActiveMembers_storesZero() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildEmptyProfilesResponse());

        engine.computeFamilyNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0.0, payload.path("family_net_worth").asDouble(), 0.001);
        assertEquals(0, payload.path("member_count").asInt());
    }

    // ── computeFormulaGoals (ADR-022 Phase 1 — corrected formulas) ──────────

    @Test
    void isGoalAchieved_thirtySeventyTarget_isLowerIsBetter() {
        assertTrue(engine.isGoalAchieved("THIRTY_SEVENTY_TARGET", 25.0, 30.0));
        assertTrue(engine.isGoalAchieved("THIRTY_SEVENTY_TARGET", 30.0, 30.0));
        assertTrue(engine.isGoalAchieved("THIRTY_SEVENTY_TARGET", 0.0, 30.0));
        assertFalse(engine.isGoalAchieved("THIRTY_SEVENTY_TARGET", 35.0, 30.0));
    }

    @Test
    void isGoalAchieved_everyOtherGoalType_isHigherIsBetter() {
        // Includes DEBT_CROSSOVER — the old code's single "< " exception, now flipped.
        for (String goalId : List.of("DEBT_CROSSOVER", "FREEDOM_RUNWAY", "INSURANCE_FREE", "YEAR_ONE")) {
            assertTrue(engine.isGoalAchieved(goalId, 100.0, 100.0), goalId + " at exactly target should be achieved");
            assertTrue(engine.isGoalAchieved(goalId, 150.0, 100.0), goalId + " above target should be achieved");
            assertFalse(engine.isGoalAchieved(goalId, 99.0, 100.0), goalId + " below target should not be achieved");
        }
    }

    @Test
    void computeFormulaGoals_noMembersNoPolicy_fourGoalsOnly_totalCountNotFixedAtFive() throws Exception {
        // No household members, no policy, no goal_plan rows configured for YEAR_ONE
        // → the 4 singleton goals only. total_count is no longer hardcoded at 5.
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildEmptyProfilesResponse());
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(4, payload.path("total_count").asInt());
        assertEquals(4, payload.path("goals").size());

        JsonNode goals = payload.path("goals");
        List<String> goalIds = new java.util.ArrayList<>();
        goals.forEach(g -> goalIds.add(g.path("goal_id").asText()));
        assertEquals(List.of("DEBT_CROSSOVER", "THIRTY_SEVENTY_TARGET", "FREEDOM_RUNWAY", "INSURANCE_FREE"), goalIds);
        // With zero debt and zero corpus, DEBT_CROSSOVER's ratio defaults to 0 → not achieved (0 < 100)
        assertEquals("IN_PROGRESS", goals.get(0).path("status").asText());
    }

    @Test
    void computeFormulaGoals_debtCrossover_selfSpouseMfCorpus_excludesChildLoansAndAccounts() throws Exception {
        UUID spouseId = UUID.fromString("77777777-0000-0000-0000-000000000002");
        UUID childId = UUID.fromString("77777777-0000-0000-0000-000000000003");
        UUID selfMfAccount = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        UUID spouseMfAccount = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
        UUID selfLoanAccount = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
        UUID childLoanAccount = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF"),
                new MemberEntry(spouseId, "Shweta", "SPOUSE"),
                new MemberEntry(childId, "Gayan", "CHILD")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsWithTypes(entry(selfMfAccount, "MUTUAL_FUND"), entry(selfLoanAccount, "HOME_LOAN")));
        when(wealthClient.listAccounts(isNull(), eq(true), eq(spouseId.toString())))
                .thenReturn(accountsWithTypes(entry(spouseMfAccount, "MUTUAL_FUND")));
        when(wealthClient.listAccounts(isNull(), eq(true), eq(childId.toString())))
                .thenReturn(accountsWithTypes(entry(childLoanAccount, "PERSONAL_LOAN")));

        when(wealthClient.getAccountBalance(selfMfAccount, PROFILE_ID.toString())).thenReturn(balanceNode(selfMfAccount, 100000.0));
        when(wealthClient.getAccountBalance(spouseMfAccount, spouseId.toString())).thenReturn(balanceNode(spouseMfAccount, 50000.0));
        when(wealthClient.getAmortization(selfLoanAccount, PROFILE_ID.toString())).thenReturn(amortizationNode(100000.0, 5000.0));
        when(wealthClient.getAmortization(childLoanAccount, childId.toString())).thenReturn(amortizationNode(999999.0, 1.0));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(0);

        assertEquals("DEBT_CROSSOVER", goal.path("goal_id").asText());
        // MF corpus (self+spouse) = 150000; outstanding loans (child excluded) = 100000 → 150%
        assertEquals(150.0, goal.path("current_value").asDouble(), 0.001);
        assertEquals(100.0, goal.path("target_value").asDouble(), 0.001);
        assertEquals("ACHIEVED", goal.path("status").asText());
    }

    @Test
    void computeFormulaGoals_freedomRunway_excludesPpfAndChildAccounts() throws Exception {
        UUID childId = UUID.fromString("77777777-0000-0000-0000-000000000003");
        UUID liquidAccount = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
        UUID ppfAccount = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
        UUID childAccount = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF"),
                new MemberEntry(childId, "Gayan", "CHILD")));
        ObjectNode policy = MAPPER.createObjectNode();
        policy.put("monthly_budget_cap", 1000.0);
        policy.put("freedom_runway_months", 360.0);
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(policy));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsWithTiers(
                        tierEntry(liquidAccount, "SAVINGS", "LIQUID"),
                        tierEntry(ppfAccount, "PPF", "SEMI_LIQUID")));
        when(wealthClient.listAccounts(isNull(), eq(true), eq(childId.toString())))
                .thenReturn(accountsWithTiers(tierEntry(childAccount, "SAVINGS", "LIQUID")));

        when(wealthClient.getAccountBalance(liquidAccount, PROFILE_ID.toString())).thenReturn(balanceNode(liquidAccount, 6000.0));
        when(wealthClient.getAccountBalance(ppfAccount, PROFILE_ID.toString())).thenReturn(balanceNode(ppfAccount, 999999.0));
        when(wealthClient.getAccountBalance(childAccount, childId.toString())).thenReturn(balanceNode(childAccount, 999999.0));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(2);

        assertEquals("FREEDOM_RUNWAY", goal.path("goal_id").asText());
        // Only the non-PPF, non-CHILD LIQUID account (6000) / 1000 monthly cap = 6 months
        assertEquals(6.0, goal.path("current_value").asDouble(), 0.001);
        assertEquals(360.0, goal.path("target_value").asDouble(), 0.001);
        assertEquals("IN_PROGRESS", goal.path("status").asText());
    }

    @Test
    void computeFormulaGoals_insuranceFree_sumsMaxGainAndFdBalances() throws Exception {
        UUID fdAccount = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
        UUID maxGainAccount = UUID.fromString("cccccccc-0000-0000-0000-000000000002");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        ObjectNode policy = MAPPER.createObjectNode();
        policy.put("insurance_free_legal_fees", 50000.0);
        policy.put("insurance_free_academic_buffer", 50000.0);
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(policy));

        List<DashboardSnapshotDto> dtos = List.of(
                buildSnapshotDto(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY,
                        "{\"total_monthly_emi\":0.0,\"total_outstanding_balance\":400000.0,\"member_count\":1}")
        );
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(dtos);

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsWithPurposeTag(fdAccount, "FD", null, maxGainAccount, "SAVINGS", "MaxGain"));
        when(wealthClient.getAccountBalance(fdAccount, PROFILE_ID.toString())).thenReturn(balanceNode(fdAccount, 300000.0));
        when(wealthClient.getAccountBalance(maxGainAccount, PROFILE_ID.toString())).thenReturn(balanceNode(maxGainAccount, 200000.0));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(3);

        assertEquals("INSURANCE_FREE", goal.path("goal_id").asText());
        // (300000 + 200000) / (400000 + 50000 + 50000) * 100 = 100%
        assertEquals(100.0, goal.path("current_value").asDouble(), 0.001);
        assertEquals("ACHIEVED", goal.path("status").asText());
    }

    @Test
    void computeFormulaGoals_thirtySeventyTarget_aggregatesEmiDebitAndIncomeCategories() throws Exception {
        UUID accountId = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));

        List<DashboardSnapshotDto> dtos = List.of(
                buildSnapshotDto(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY,
                        "{\"total_monthly_emi\":1000.0,\"total_outstanding_balance\":0.0,\"member_count\":1}")
        );
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(dtos);

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("DEBIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("HOUSEHOLD_CORE", 3000.0));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        assertEquals("THIRTY_SEVENTY_TARGET", goal.path("goal_id").asText());
        // numerator = 1000 (emi) + 3000/3 (debit avg) + 0 (insurance) = 2000
        // denominator = 9000/3 (income avg) = 3000 → 2000/3000*100 = 66.67%
        assertEquals(66.667, goal.path("current_value").asDouble(), 0.01);
        assertEquals(30.0, goal.path("target_value").asDouble(), 0.001);
        // 66.67 > 30 → NOT achieved (lower-is-better exception)
        assertEquals("IN_PROGRESS", goal.path("status").asText());
    }

    // ── computeFormulaGoals — THIRTY_SEVENTY_TARGET insurance premium wiring (ADR-022 Phase 2) ──

    @Test
    void computeFormulaGoals_thirtySeventyTarget_monthlyPremium_passesThroughUnchanged() throws Exception {
        UUID accountId = UUID.fromString("ffffffff-0000-0000-0000-000000000001");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));
        when(wealthClient.listInsurancePolicies(ADMIN_ID))
                .thenReturn(insurancePoliciesResponse(new PolicyEntry(500.0, "MONTHLY", true)));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        assertEquals("THIRTY_SEVENTY_TARGET", goal.path("goal_id").asText());
        // numerator = 0 (emi) + 0 (debit avg) + 500 (MONTHLY premium, unchanged) = 500
        // denominator = 9000/3 (income avg) = 3000 → 500/3000*100 = 16.667%
        assertEquals(16.667, goal.path("current_value").asDouble(), 0.01);
    }

    @Test
    void computeFormulaGoals_thirtySeventyTarget_annualPremium_normalizedByDividingTwelve() throws Exception {
        UUID accountId = UUID.fromString("ffffffff-0000-0000-0000-000000000002");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));
        when(wealthClient.listInsurancePolicies(ADMIN_ID))
                .thenReturn(insurancePoliciesResponse(new PolicyEntry(12000.0, "ANNUAL", true)));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        assertEquals("THIRTY_SEVENTY_TARGET", goal.path("goal_id").asText());
        // 12000/year ÷ 12 = 1000/month; numerator = 0 + 0 + 1000 = 1000
        // denominator = 9000/3 = 3000 → 1000/3000*100 = 33.33%
        assertEquals(33.333, goal.path("current_value").asDouble(), 0.01);
    }

    @Test
    void computeFormulaGoals_thirtySeventyTarget_mixedFrequencies_bothNormalizedAndSummed() throws Exception {
        UUID accountId = UUID.fromString("ffffffff-0000-0000-0000-000000000003");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));
        // MONTHLY 300 (unchanged) + ANNUAL 2400 (÷12 = 200) = 500 total monthly premium
        when(wealthClient.listInsurancePolicies(ADMIN_ID))
                .thenReturn(insurancePoliciesResponse(
                        new PolicyEntry(300.0, "MONTHLY", true),
                        new PolicyEntry(2400.0, "ANNUAL", true)));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        assertEquals(16.667, goal.path("current_value").asDouble(), 0.01);
    }

    @Test
    void computeFormulaGoals_thirtySeventyTarget_inactiveInsurancePolicy_excludedFromSum() throws Exception {
        UUID accountId = UUID.fromString("ffffffff-0000-0000-0000-000000000004");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));
        // Active 500 counts; inactive 5000 must not
        when(wealthClient.listInsurancePolicies(ADMIN_ID))
                .thenReturn(insurancePoliciesResponse(
                        new PolicyEntry(500.0, "MONTHLY", true),
                        new PolicyEntry(5000.0, "MONTHLY", false)));

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        assertEquals(16.667, goal.path("current_value").asDouble(), 0.01);
    }

    @Test
    void computeFormulaGoals_thirtySeventyTarget_insurancePolicyLookupFails_treatsPremiumsAsZero() throws Exception {
        UUID accountId = UUID.fromString("ffffffff-0000-0000-0000-000000000005");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.listTransactions(eq(accountId), eq(PROFILE_ID.toString()), anyString(), anyString(),
                eq("CREDIT"), eq(0), eq(200)))
                .thenReturn(transactionsResponse("SALARY", 9000.0));
        when(wealthClient.listInsurancePolicies(ADMIN_ID)).thenThrow(new RuntimeException("wealth service unavailable"));

        assertDoesNotThrow(() -> engine.computeFormulaGoals(PROFILE_ID));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode goal = payload(payloadCaptor).path("goals").get(1);

        // insurance term defaults to 0 on lookup failure, same tolerant pattern as YEAR_ONE's listGoalPlans
        assertEquals(0.0, goal.path("current_value").asDouble(), 0.01);
    }

    @Test
    void computeFormulaGoals_yearOne_perChild_zeroMfBalanceStillGetsEntry_unconfiguredChildOmitted() throws Exception {
        UUID configuredChild = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001");
        UUID unconfiguredChild = UUID.fromString("eeeeeeee-0000-0000-0000-000000000002");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF"),
                new MemberEntry(configuredChild, "Aanya", "CHILD"),
                new MemberEntry(unconfiguredChild, "Zoya", "CHILD")));
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        ObjectNode plan = MAPPER.createObjectNode();
        plan.put("goal_type", "YEAR_ONE");
        plan.put("beneficiary_profile_id", configuredChild.toString());
        plan.put("education_base_cost", 1000000.0);
        plan.put("education_inflation_rate", 0.0);
        plan.put("education_years_to_entry", 0);
        ArrayNode plans = MAPPER.createArrayNode();
        plans.add(plan);
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(MAPPER.createObjectNode().set("goal_plans", plans));

        // configuredChild has zero MUTUAL_FUND accounts — buildEmptyAccountsResponse default stub covers it
        when(wealthClient.listAccounts(isNull(), eq(true), eq(configuredChild.toString())))
                .thenReturn(buildEmptyAccountsResponse());

        engine.computeFormulaGoals(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY), payloadCaptor.capture());
        JsonNode payload = payload(payloadCaptor);

        // 4 singleton goals + exactly 1 YEAR_ONE entry (unconfigured child omitted entirely)
        assertEquals(5, payload.path("total_count").asInt());
        JsonNode goals = payload.path("goals");
        JsonNode yearOneGoal = goals.get(4);
        assertEquals("YEAR_ONE", yearOneGoal.path("goal_id").asText());
        assertEquals("Year One — Aanya", yearOneGoal.path("goal_name").asText());
        assertEquals(configuredChild.toString(), yearOneGoal.path("beneficiary_profile_id").asText());
        assertEquals("Aanya", yearOneGoal.path("beneficiary_name").asText());
        // Zero MF balance → current_value=0, still shown (not omitted) per ADR-022
        assertEquals(0.0, yearOneGoal.path("current_value").asDouble(), 0.001);
        assertEquals("IN_PROGRESS", yearOneGoal.path("status").asText());

        for (JsonNode goal : goals) {
            assertNotEquals("Zoya", goal.path("beneficiary_name").asText(),
                    "Unconfigured child Zoya must not appear anywhere in the payload");
        }
    }

    // ── computeGoalDetail (ADR-022 Phase 3) ──────────────────────────────────

    @Test
    void computeGoalDetail_singletonGoal_mergesConfigAndComputesNonChecklistMilestoneStatus() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("DEBT_CROSSOVER", 150.0, 100.0, "ACHIEVED", "percent", null))));

        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("baseline_debt", "400000");
        ObjectNode plan = goalPlanNode("DEBT_CROSSOVER", null, "Reduce debt below MF corpus", "Debt fully covered", detail);
        ArrayNode milestones = MAPPER.createArrayNode();
        milestones.add(milestoneNode("m1", 1, "Halfway there", 75.0, false, false, "Corpus covers half the debt"));
        plan.set("milestones", milestones);
        ArrayNode rules = MAPPER.createArrayNode();
        rules.add(ruleNode(1, "No Liquidation", "Never liquidate the MF corpus early"));
        plan.set("rules", rules);
        ArrayNode triggerEvents = MAPPER.createArrayNode();
        triggerEvents.add(triggerEventNode(1, "Bonus received", "Annual bonus credited", "Lump-sum into MF corpus"));
        plan.set("trigger_events", triggerEvents);
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(plan));

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        JsonNode goalDetails = payload(payloadCaptor).path("goal_details");
        assertEquals(1, goalDetails.size());

        JsonNode entry = goalDetails.get(0);
        assertEquals("DEBT_CROSSOVER", entry.path("goal_id").asText());
        assertEquals("Reduce debt below MF corpus", entry.path("objective").asText());
        assertEquals("Debt fully covered", entry.path("target_state").asText());
        assertEquals("400000", entry.path("detail").path("baseline_debt").asText());
        assertEquals(150.0, entry.path("current_value").asDouble(), 0.001);
        assertEquals(100.0, entry.path("target_value").asDouble(), 0.001);
        assertEquals("ACHIEVED", entry.path("status").asText());
        assertFalse(entry.has("insurance_policies"), "Non-INSURANCE_FREE goals must not carry a policy list");

        JsonNode milestone = entry.path("milestones").get(0);
        assertEquals("Halfway there", milestone.path("label").asText());
        // DEBT_CROSSOVER is "higher is better"; 150 >= 75 → achieved
        assertTrue(milestone.path("is_achieved").asBoolean());
        assertFalse(milestone.path("is_manual_checklist").asBoolean());

        assertEquals("No Liquidation", entry.path("rules").get(0).path("rule_name").asText());
        assertEquals("Bonus received", entry.path("trigger_events").get(0).path("event_name").asText());
    }

    @Test
    void computeGoalDetail_yearOne_perChild_joinsByBeneficiaryProfileId() throws Exception {
        UUID childA = UUID.fromString("dddddddd-1111-0000-0000-000000000001");
        UUID childB = UUID.fromString("dddddddd-1111-0000-0000-000000000002");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(
                        formulaGoalNode("YEAR_ONE", 40.0, 100.0, "IN_PROGRESS", "percent", childA),
                        formulaGoalNode("YEAR_ONE", 120.0, 100.0, "ACHIEVED", "percent", childB))));

        ObjectNode planA = goalPlanNode("YEAR_ONE", childA, "Fund Aanya's first year", null, MAPPER.createObjectNode());
        planA.set("milestones", MAPPER.createArrayNode());
        planA.set("rules", MAPPER.createArrayNode());
        planA.set("trigger_events", MAPPER.createArrayNode());
        ObjectNode planB = goalPlanNode("YEAR_ONE", childB, "Fund Zoya's first year", null, MAPPER.createObjectNode());
        planB.set("milestones", MAPPER.createArrayNode());
        planB.set("rules", MAPPER.createArrayNode());
        planB.set("trigger_events", MAPPER.createArrayNode());
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(planA, planB));

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        JsonNode goalDetails = payload(payloadCaptor).path("goal_details");
        assertEquals(2, goalDetails.size());

        JsonNode entryA = findByBeneficiary(goalDetails, childA);
        assertEquals("Fund Aanya's first year", entryA.path("objective").asText());
        assertEquals(40.0, entryA.path("current_value").asDouble(), 0.001);

        JsonNode entryB = findByBeneficiary(goalDetails, childB);
        assertEquals("Fund Zoya's first year", entryB.path("objective").asText());
        assertEquals(120.0, entryB.path("current_value").asDouble(), 0.001);
    }

    @Test
    void computeGoalDetail_manualChecklistMilestone_usesOwnIsAchieved_notRecomputed() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("FREEDOM_RUNWAY", 2.0, 360.0, "IN_PROGRESS", "months", null))));

        ObjectNode plan = goalPlanNode("FREEDOM_RUNWAY", null, "Build a 30-year runway", null, MAPPER.createObjectNode());
        ArrayNode milestones = MAPPER.createArrayNode();
        // is_manual_checklist=true, target_value omitted entirely, is_achieved=true —
        // current_value (2.0) is nowhere near a formula-derived "achieved" outcome, proving
        // this status is NOT recomputed from current_value.
        ObjectNode checklistMilestone = MAPPER.createObjectNode();
        checklistMilestone.put("id", "m-checklist");
        checklistMilestone.put("sequence_no", 1);
        checklistMilestone.put("label", "Opened emergency fund account");
        checklistMilestone.put("is_manual_checklist", true);
        checklistMilestone.put("is_achieved", true);
        checklistMilestone.put("significance", "First concrete step");
        milestones.add(checklistMilestone);
        plan.set("milestones", milestones);
        plan.set("rules", MAPPER.createArrayNode());
        plan.set("trigger_events", MAPPER.createArrayNode());
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(plan));

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        JsonNode milestone = payload(payloadCaptor).path("goal_details").get(0).path("milestones").get(0);

        assertTrue(milestone.path("is_manual_checklist").asBoolean());
        assertTrue(milestone.path("is_achieved").asBoolean());
        assertFalse(milestone.has("target_value"), "Checklist milestones with no target_value must omit the field, not default to 0");
    }

    @Test
    void computeGoalDetail_thirtySeventyTarget_milestoneRespectsLowerIsBetterException() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("THIRTY_SEVENTY_TARGET", 25.0, 30.0, "ACHIEVED", "percent", null))));

        ObjectNode plan = goalPlanNode("THIRTY_SEVENTY_TARGET", null, "Keep essentials under 30%", null, MAPPER.createObjectNode());
        ArrayNode milestones = MAPPER.createArrayNode();
        // 25 <= 28 → achieved (lower-is-better)
        milestones.add(milestoneNode("m-achieved", 1, "Under 28%", 28.0, false, false, "First checkpoint"));
        // 25 <= 20 is false → not achieved
        milestones.add(milestoneNode("m-not-achieved", 2, "Under 20%", 20.0, false, false, "Stretch checkpoint"));
        plan.set("milestones", milestones);
        plan.set("rules", MAPPER.createArrayNode());
        plan.set("trigger_events", MAPPER.createArrayNode());
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(plan));

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        JsonNode milestones2 = payload(payloadCaptor).path("goal_details").get(0).path("milestones");

        assertTrue(milestones2.get(0).path("is_achieved").asBoolean());
        assertFalse(milestones2.get(1).path("is_achieved").asBoolean());
    }

    @Test
    void computeGoalDetail_insuranceFree_attachesOnlyActivePolicies() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("INSURANCE_FREE", 100.0, 100.0, "ACHIEVED", "percent", null))));

        ObjectNode plan = goalPlanNode("INSURANCE_FREE", null, "Zero reliance on insurance payout", null, MAPPER.createObjectNode());
        plan.set("milestones", MAPPER.createArrayNode());
        plan.set("rules", MAPPER.createArrayNode());
        plan.set("trigger_events", MAPPER.createArrayNode());
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(plan));

        ArrayNode policies = MAPPER.createArrayNode();
        policies.add(insurancePolicyNode("LIC", "Term Cover", "TERM", 1000.0, "MONTHLY", 5000000.0, true));
        policies.add(insurancePolicyNode("HDFC Life", "Old Endowment", "ENDOWMENT", 500.0, "MONTHLY", 1000000.0, false));
        when(wealthClient.listInsurancePolicies(ADMIN_ID)).thenReturn(MAPPER.createObjectNode().set("insurance_policies", policies));

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        JsonNode insurancePolicies = payload(payloadCaptor).path("goal_details").get(0).path("insurance_policies");

        assertEquals(1, insurancePolicies.size());
        JsonNode policy = insurancePolicies.get(0);
        assertEquals("LIC", policy.path("provider").asText());
        assertEquals("Term Cover", policy.path("policy_name").asText());
        assertEquals("TERM", policy.path("policy_type").asText());
        assertEquals(1000.0, policy.path("premium_amount").asDouble(), 0.001);
        assertEquals("MONTHLY", policy.path("premium_frequency").asText());
        assertEquals(5000000.0, policy.path("coverage_amount").asDouble(), 0.001);
    }

    @Test
    void computeGoalDetail_goalPlanRowWithNoLiveMatch_skippedNotErrored() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        // Only DEBT_CROSSOVER is live this pass — FREEDOM_RUNWAY has no matching formula-goal entry
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("DEBT_CROSSOVER", 150.0, 100.0, "ACHIEVED", "percent", null))));

        ObjectNode orphanPlan = goalPlanNode("FREEDOM_RUNWAY", null, "Never matched", null, MAPPER.createObjectNode());
        orphanPlan.set("milestones", MAPPER.createArrayNode());
        orphanPlan.set("rules", MAPPER.createArrayNode());
        orphanPlan.set("trigger_events", MAPPER.createArrayNode());
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenReturn(goalPlansResponse(orphanPlan));

        assertDoesNotThrow(() -> engine.computeGoalDetail(PROFILE_ID));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        assertEquals(0, payload(payloadCaptor).path("goal_details").size());
    }

    @Test
    void computeGoalDetail_noGoalPlansConfigured_writesEmptyGoalDetailsArray() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("DEBT_CROSSOVER", 150.0, 100.0, "ACHIEVED", "percent", null))));
        // wealthClient.listGoalPlans defaults to an empty goal_plans array (setUp())

        engine.computeGoalDetail(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), payloadCaptor.capture());
        assertEquals(0, payload(payloadCaptor).path("goal_details").size());
    }

    @Test
    void computeGoalDetail_noAdminId_returnsEarlyWithoutUpsert() throws Exception {
        ObjectNode profileWithoutAdmin = MAPPER.createObjectNode();
        profileWithoutAdmin.put("profile_id", PROFILE_ID.toString());
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(profileWithoutAdmin);

        engine.computeGoalDetail(PROFILE_ID);

        verify(snapshotRepo, never()).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), anyString());
    }

    @Test
    void computeGoalDetail_goalPlanLookupFails_doesNotThrow_skipsSnapshot() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of(
                formulaGoalsSnapshotDto(formulaGoalNode("DEBT_CROSSOVER", 150.0, 100.0, "ACHIEVED", "percent", null))));
        when(wealthClient.listGoalPlans(ADMIN_ID)).thenThrow(new RuntimeException("wealth service unavailable"));

        assertDoesNotThrow(() -> engine.computeGoalDetail(PROFILE_ID));

        verify(snapshotRepo, never()).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY), anyString());
    }

    // ── computeGoalDetail test helpers ───────────────────────────────────────

    private JsonNode findByBeneficiary(JsonNode goalDetails, UUID beneficiaryProfileId) {
        for (JsonNode entry : goalDetails) {
            if (beneficiaryProfileId.toString().equals(entry.path("beneficiary_profile_id").asText())) {
                return entry;
            }
        }
        throw new AssertionError("No goal_details entry found for beneficiary " + beneficiaryProfileId);
    }

    private DashboardSnapshotDto formulaGoalsSnapshotDto(ObjectNode... goals) {
        ArrayNode array = MAPPER.createArrayNode();
        for (ObjectNode goal : goals) {
            array.add(goal);
        }
        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("goals", array);
        return buildSnapshotDto(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY, payload.toString());
    }

    private ObjectNode formulaGoalNode(String goalId, double currentValue, double targetValue, String status,
                                        String unit, UUID beneficiaryProfileId) {
        ObjectNode goal = MAPPER.createObjectNode();
        goal.put("goal_id", goalId);
        goal.put("goal_name", goalId);
        goal.put("status", status);
        goal.put("description", goalId + " description");
        goal.put("current_value", currentValue);
        goal.put("target_value", targetValue);
        goal.put("unit", unit);
        if (beneficiaryProfileId != null) {
            goal.put("beneficiary_profile_id", beneficiaryProfileId.toString());
            goal.put("beneficiary_name", "Child");
        }
        return goal;
    }

    private ObjectNode goalPlanNode(String goalType, UUID beneficiaryProfileId, String objective,
                                     String targetState, ObjectNode detail) {
        ObjectNode plan = MAPPER.createObjectNode();
        plan.put("goal_type", goalType);
        if (beneficiaryProfileId != null) {
            plan.put("beneficiary_profile_id", beneficiaryProfileId.toString());
        }
        plan.put("objective", objective);
        if (targetState != null) {
            plan.put("target_state", targetState);
        }
        plan.set("detail", detail);
        return plan;
    }

    private ObjectNode milestoneNode(String id, int sequenceNo, String label, Double targetValue,
                                      boolean isManualChecklist, boolean isAchieved, String significance) {
        ObjectNode milestone = MAPPER.createObjectNode();
        milestone.put("id", id);
        milestone.put("sequence_no", sequenceNo);
        milestone.put("label", label);
        if (targetValue != null) {
            milestone.put("target_value", targetValue);
        }
        milestone.put("is_manual_checklist", isManualChecklist);
        milestone.put("is_achieved", isAchieved);
        milestone.put("significance", significance);
        return milestone;
    }

    private ObjectNode ruleNode(int sequenceNo, String ruleName, String ruleText) {
        ObjectNode rule = MAPPER.createObjectNode();
        rule.put("sequence_no", sequenceNo);
        rule.put("rule_name", ruleName);
        rule.put("rule_text", ruleText);
        return rule;
    }

    private ObjectNode triggerEventNode(int sequenceNo, String eventName, String triggerCondition, String resultingChange) {
        ObjectNode trigger = MAPPER.createObjectNode();
        trigger.put("sequence_no", sequenceNo);
        trigger.put("event_name", eventName);
        trigger.put("trigger_condition", triggerCondition);
        trigger.put("resulting_change", resultingChange);
        return trigger;
    }

    private ObjectNode insurancePolicyNode(String provider, String policyName, String policyType,
                                            double premiumAmount, String premiumFrequency,
                                            Double coverageAmount, boolean isActive) {
        ObjectNode policy = MAPPER.createObjectNode();
        policy.put("id", UUID.randomUUID().toString());
        policy.put("provider", provider);
        policy.put("policy_name", policyName);
        policy.put("policy_type", policyType);
        policy.put("premium_amount", premiumAmount);
        policy.put("premium_frequency", premiumFrequency);
        if (coverageAmount != null) {
            policy.put("coverage_amount", coverageAmount);
        }
        policy.set("payout_structure", MAPPER.createObjectNode());
        policy.put("is_active", isActive);
        return policy;
    }

    private JsonNode goalPlansResponse(ObjectNode... plans) {
        ArrayNode array = MAPPER.createArrayNode();
        for (ObjectNode plan : plans) {
            array.add(plan);
        }
        return MAPPER.createObjectNode().set("goal_plans", array);
    }

    // ── computeValidation (Epic 8 Phase 4) ───────────────────────────────────

    @Test
    void computeValidation_warningWhenCategoryUncategorized() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(MAPPER.createObjectNode()));

        // Category validation snapshot: 3 uncategorized accounts
        List<DashboardSnapshotDto> dtos = List.of(
                buildSnapshotDto(SnapshotKey.WEALTH_CATEGORY_VALIDATION,
                        "{\"total_accounts\":3,\"categorized_count\":0,\"uncategorized_count\":3}"),
                buildSnapshotDto(SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY,
                        "{\"total_current_value\":0.0,\"projections\":[]}"),
                buildSnapshotDto(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY,
                        "{\"total_monthly_emi\":0.0,\"member_count\":0}")
        );
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(dtos);

        engine.computeValidation(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_VALIDATION_REPORT_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals("WARNING", payload.path("overall_status").asText());
        JsonNode checks = payload.path("checks");
        // First check is CATEGORY_RESOLUTION → WARNING
        assertEquals("CATEGORY_RESOLUTION", checks.get(0).path("check_id").asText());
        assertEquals("WARNING", checks.get(0).path("status").asText());
        // warning_count >= 1 (at least category + budget cap since no policy set)
        assertTrue(payload.path("warning_count").asInt() >= 1);
    }

    @Test
    void computeValidation_passWhenAllChecksClean() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());

        // Policy with budget cap set
        ObjectNode policy = MAPPER.createObjectNode();
        policy.put("monthly_budget_cap", 50000.0);
        when(profileClient.getAdmin(ADMIN_ID)).thenReturn(buildAdminResponse(policy));

        // All snapshots clean
        List<DashboardSnapshotDto> dtos = List.of(
                buildSnapshotDto(SnapshotKey.WEALTH_CATEGORY_VALIDATION,
                        "{\"total_accounts\":2,\"categorized_count\":2,\"uncategorized_count\":0}"),
                buildSnapshotDto(SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY,
                        "{\"total_current_value\":100000.0,\"projections\":[{\"account_id\":\"aaa\",\"growth_rate_annual\":0.12}]}"),
                buildSnapshotDto(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY,
                        "{\"total_monthly_emi\":5000.0,\"member_count\":1}")
        );
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(dtos);

        engine.computeValidation(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_VALIDATION_REPORT_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals("PASS", payload.path("overall_status").asText());
        assertEquals(4, payload.path("pass_count").asInt());
        assertEquals(0, payload.path("warning_count").asInt());
        assertEquals(0, payload.path("critical_count").asInt());
    }

    // ── computeActionCenterAlerts (v0.5 Phase 3, Q30) ────────────────────────

    @Test
    void computeActionCenterAlerts_aggregatesUpcomingEventsAcrossMembers() throws Exception {
        UUID spouseProfileId = UUID.fromString("99999999-0000-0000-0000-000000000002");
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true)).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF"),
                new MemberEntry(spouseProfileId, "Shweta", "SPOUSE")
        ));
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(2));
        when(householdClient.listCalendarEvents(eq(spouseProfileId), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(1));
        stubNoVehicles(PROFILE_ID);
        stubNoVehicles(spouseProfileId);
        stubNoVitals(PROFILE_ID);
        stubNoVitals(spouseProfileId);

        engine.computeActionCenterAlerts(PROFILE_ID, FIXED_TODAY);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(3, payload.path("upcoming_events").size());
        assertEquals(2, payload.path("member_count").asInt());
    }

    @Test
    void computeActionCenterAlerts_flagsVehicleExpiringWithinThirtyDays() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true))
                .thenReturn(buildProfilesResponse(new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(0));
        stubNoVitals(PROFILE_ID);

        String nearExpiry = FIXED_TODAY.plusDays(10).toString();
        when(wealthClient.listPhysicalAssets("VEHICLE", true, PROFILE_ID.toString(), null, null))
                .thenReturn(buildPhysicalAssetsResponse("a1", "Tata Nexon", nearExpiry, null));

        engine.computeActionCenterAlerts(PROFILE_ID, FIXED_TODAY);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        JsonNode compliance = payload.path("vehicle_compliance");
        assertEquals(1, compliance.size());
        assertEquals("Tata Nexon", compliance.get(0).path("asset_name").asText());
        assertEquals("PUC_EXPIRED", compliance.get(0).path("issue_type").asText());
        assertEquals("Ketan", compliance.get(0).path("full_name").asText());
    }

    @Test
    void computeActionCenterAlerts_ignoresVehicleExpiringBeyondThirtyDays() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true))
                .thenReturn(buildProfilesResponse(new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(0));
        stubNoVitals(PROFILE_ID);

        String farExpiry = FIXED_TODAY.plusDays(90).toString();
        when(wealthClient.listPhysicalAssets("VEHICLE", true, PROFILE_ID.toString(), null, null))
                .thenReturn(buildPhysicalAssetsResponse("a1", "Tata Nexon", farExpiry, farExpiry));

        engine.computeActionCenterAlerts(PROFILE_ID, FIXED_TODAY);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), payloadCaptor.capture());

        assertTrue(MAPPER.readTree(payloadCaptor.getValue()).path("vehicle_compliance").isEmpty());
    }

    @Test
    void computeActionCenterAlerts_flagsStreakGapWhenLastReadingOverThirtyDaysAgo() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true))
                .thenReturn(buildProfilesResponse(new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(0));
        stubNoVehicles(PROFILE_ID);

        String staleDate = FIXED_TODAY.minusDays(45).toString();
        when(healthClient.listVitals(eq(PROFILE_ID), isNull(), anyInt(), anyInt())).thenReturn(
                buildVitalsResponse(new VitalEntry("WEIGHT", 70.0, 0.0, "kg", staleDate)));

        engine.computeActionCenterAlerts(PROFILE_ID, FIXED_TODAY);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), payloadCaptor.capture());

        JsonNode gaps = MAPPER.readTree(payloadCaptor.getValue()).path("biometric_streak_gaps");
        // 3 core vital types tracked (Q30); WEIGHT has a stale reading, the other two were never logged — all 3 gap.
        assertEquals(3, gaps.size());
        JsonNode weightGap = findGapByType(gaps, "WEIGHT");
        assertEquals(45, weightGap.path("days_since_last_reading").asInt());
        assertEquals(staleDate, weightGap.path("last_reading_date").asText());
        JsonNode neverLoggedGap = findGapByType(gaps, "BLOOD_PRESSURE");
        assertTrue(neverLoggedGap.path("last_reading_date").isNull());
    }

    @Test
    void computeActionCenterAlerts_noStreakGapWhenAllCoreVitalsRecentlyLogged() throws Exception {
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(ADMIN_ID, true))
                .thenReturn(buildProfilesResponse(new MemberEntry(PROFILE_ID, "Ketan", "SELF")));
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(buildCalendarEventsResponse(0));
        stubNoVehicles(PROFILE_ID);

        String recentDate = FIXED_TODAY.minusDays(5).toString();
        when(healthClient.listVitals(eq(PROFILE_ID), isNull(), anyInt(), anyInt())).thenReturn(buildVitalsResponse(
                new VitalEntry("WEIGHT", 70.0, 0.0, "kg", recentDate),
                new VitalEntry("BLOOD_PRESSURE", 120.0, 80.0, "mmHg", recentDate),
                new VitalEntry("BLOOD_SUGAR_FASTING", 95.0, 0.0, "mg/dL", recentDate)
        ));

        engine.computeActionCenterAlerts(PROFILE_ID, FIXED_TODAY);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.ACTION_CENTER_ALERTS_FAMILY), payloadCaptor.capture());

        assertTrue(MAPPER.readTree(payloadCaptor.getValue()).path("biometric_streak_gaps").isEmpty());
    }

    private JsonNode findGapByType(JsonNode gaps, String vitalType) {
        for (JsonNode gap : gaps) {
            if (vitalType.equals(gap.path("vital_type").asText())) {
                return gap;
            }
        }
        throw new AssertionError("No gap entry found for vital type " + vitalType);
    }

    private void stubNoVehicles(UUID profileId) {
        when(wealthClient.listPhysicalAssets("VEHICLE", true, profileId.toString(), null, null))
                .thenReturn(MAPPER.createObjectNode().set("physical_assets", MAPPER.createArrayNode()));
    }

    private void stubNoVitals(UUID profileId) {
        when(healthClient.listVitals(eq(profileId), isNull(), anyInt(), anyInt()))
                .thenReturn(MAPPER.createObjectNode().set("vital_readings", MAPPER.createArrayNode()));
    }

    private JsonNode buildPhysicalAssetsResponse(String assetId, String assetName, String pucExpiry, String insuranceExpiry) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode assets = MAPPER.createArrayNode();
        ObjectNode asset = MAPPER.createObjectNode();
        asset.put("asset_id", assetId);
        asset.put("asset_name", assetName);
        ObjectNode metadata = MAPPER.createObjectNode();
        if (pucExpiry != null) {
            metadata.put("puc_expiry", pucExpiry);
        }
        if (insuranceExpiry != null) {
            metadata.put("insurance_expiry", insuranceExpiry);
        }
        asset.set("metadata", metadata);
        assets.add(asset);
        root.set("physical_assets", assets);
        return root;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JsonNode buildAccountsResponse(UUID... accountIds) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode accounts = MAPPER.createArrayNode();
        for (UUID accountId : accountIds) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("account_id", accountId.toString());
            accounts.add(account);
        }
        root.set("accounts", accounts);
        return root;
    }

    private void stubBalance(UUID accountId, double currentBalance) {
        ObjectNode balance = MAPPER.createObjectNode();
        balance.put("account_id", accountId.toString());
        balance.put("current_balance", currentBalance);
        when(wealthClient.getAccountBalance(accountId, PROFILE_ID.toString())).thenReturn(balance);
    }

    private JsonNode buildEmptyAccountsResponse() {
        ObjectNode root = MAPPER.createObjectNode();
        root.set("accounts", MAPPER.createArrayNode());
        return root;
    }

    // ── ADR-022 formula-goal test helpers ───────────────────────────────────

    private record TypedAccount(UUID id, String accountType) {
    }

    private TypedAccount entry(UUID id, String accountType) {
        return new TypedAccount(id, accountType);
    }

    private JsonNode accountsWithTypes(TypedAccount... accounts) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        for (TypedAccount acc : accounts) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("account_id", acc.id().toString());
            account.put("account_type", acc.accountType());
            array.add(account);
        }
        root.set("accounts", array);
        return root;
    }

    private record TieredAccount(UUID id, String accountType, String liquidityTier) {
    }

    private TieredAccount tierEntry(UUID id, String accountType, String liquidityTier) {
        return new TieredAccount(id, accountType, liquidityTier);
    }

    private JsonNode accountsWithTiers(TieredAccount... accounts) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        for (TieredAccount acc : accounts) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("account_id", acc.id().toString());
            account.put("account_type", acc.accountType());
            ObjectNode metadata = MAPPER.createObjectNode();
            metadata.put("liquidity_tier", acc.liquidityTier());
            account.set("metadata", metadata);
            array.add(account);
        }
        root.set("accounts", array);
        return root;
    }

    private JsonNode accountsWithPurposeTag(UUID id1, String type1, String purposeTag1, UUID id2, String type2, String purposeTag2) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        array.add(purposeTaggedAccount(id1, type1, purposeTag1));
        array.add(purposeTaggedAccount(id2, type2, purposeTag2));
        root.set("accounts", array);
        return root;
    }

    private ObjectNode purposeTaggedAccount(UUID id, String accountType, String purposeTag) {
        ObjectNode account = MAPPER.createObjectNode();
        account.put("account_id", id.toString());
        account.put("account_type", accountType);
        if (purposeTag != null) {
            ObjectNode metadata = MAPPER.createObjectNode();
            metadata.put("purpose_tag", purposeTag);
            account.set("metadata", metadata);
        }
        return account;
    }

    private JsonNode amortizationNode(double outstandingBalance, double monthlyEmi) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("outstanding_balance", outstandingBalance);
        node.put("monthly_emi", monthlyEmi);
        node.put("remaining_months", 12);
        node.put("interest_rate", 8.5);
        return node;
    }

    private JsonNode transactionsResponse(String category, double amount) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        ObjectNode txn = MAPPER.createObjectNode();
        txn.put("amount", amount);
        ObjectNode metadata = MAPPER.createObjectNode();
        metadata.put("category", category);
        txn.set("metadata", metadata);
        array.add(txn);
        root.set("transactions", array);
        return root;
    }

    private JsonNode insurancePoliciesResponse(PolicyEntry... entries) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode array = MAPPER.createArrayNode();
        for (PolicyEntry entry : entries) {
            ObjectNode policy = MAPPER.createObjectNode();
            policy.put("id", UUID.randomUUID().toString());
            policy.put("premium_amount", entry.premiumAmount);
            policy.put("premium_frequency", entry.frequency);
            policy.put("is_active", entry.active);
            array.add(policy);
        }
        root.set("insurance_policies", array);
        return root;
    }

    private record PolicyEntry(double premiumAmount, String frequency, boolean active) {
    }

    private JsonNode payload(ArgumentCaptor<String> captor) {
        try {
            return MAPPER.readTree(captor.getValue());
        } catch (Exception e) {
            throw new AssertionError("Failed to parse snapshot payload", e);
        }
    }

    private JsonNode buildVitalsResponse(VitalEntry... entries) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode vitals = MAPPER.createArrayNode();
        for (VitalEntry entry : entries) {
            ObjectNode vital = MAPPER.createObjectNode();
            vital.put("vital_type", entry.type);
            vital.put("value_primary", entry.primary);
            vital.put("value_secondary", entry.secondary);
            vital.put("unit", entry.unit);
            vital.put("reading_date", entry.date);
            vitals.add(vital);
        }
        root.set("vital_readings", vitals);
        return root;
    }

    private JsonNode buildCalendarEventsResponse(int count) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode events = MAPPER.createArrayNode();
        for (int i = 0; i < count; i++) {
            ObjectNode event = MAPPER.createObjectNode();
            event.put("id", UUID.randomUUID().toString());
            event.put("title", "Event " + i);
            event.put("start_date", "2026-07-0" + (i + 1));
            events.add(event);
        }
        root.set("calendar_events", events);
        return root;
    }

    private JsonNode buildGoalsResponse(UUID goalId, String name, double targetAmount) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode goals = MAPPER.createArrayNode();
        ObjectNode goal = MAPPER.createObjectNode();
        goal.put("id", goalId.toString());
        goal.put("goal_name", name);
        goal.put("target_amount", targetAmount);
        goals.add(goal);
        root.set("goals", goals);
        return root;
    }

    private record VitalEntry(String type, double primary, double secondary, String unit, String date) {
    }

    private JsonNode buildOwnProfileResponse() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("profile_id", PROFILE_ID.toString());
        root.put("admin_id", ADMIN_ID.toString());
        root.put("full_name", "Ketan");
        root.put("relation_to_admin", "SELF");
        return root;
    }

    private JsonNode buildEmptyProfilesResponse() {
        ObjectNode root = MAPPER.createObjectNode();
        root.set("profiles", MAPPER.createArrayNode());
        return root;
    }

    private JsonNode buildProfilesResponse(MemberEntry... members) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode profiles = MAPPER.createArrayNode();
        for (MemberEntry member : members) {
            ObjectNode profile = MAPPER.createObjectNode();
            profile.put("profile_id", member.profileId.toString());
            profile.put("admin_id", ADMIN_ID.toString());
            profile.put("full_name", member.fullName);
            profile.put("relation_to_admin", member.relation);
            profiles.add(profile);
        }
        root.set("profiles", profiles);
        return root;
    }

    private JsonNode balanceNode(UUID accountId, double currentBalance) {
        ObjectNode balance = MAPPER.createObjectNode();
        balance.put("account_id", accountId.toString());
        balance.put("current_balance", currentBalance);
        return balance;
    }

    private record MemberEntry(UUID profileId, String fullName, String relation) {
    }

    private JsonNode buildAdminResponse(ObjectNode policySettings) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", ADMIN_ID.toString());
        root.set("policy_settings", policySettings);
        return root;
    }

    private DashboardSnapshotDto buildSnapshotDto(String snapshotKey, String payloadJson) {
        return new DashboardSnapshotDto(PROFILE_ID, snapshotKey, payloadJson, java.time.Instant.parse("2026-06-15T00:00:00Z"));
    }
}
