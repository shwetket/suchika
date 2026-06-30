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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        verify(wealthClient).getAccountBalance(eq(accountId), eq(PROFILE_ID.toString()));
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
        when(healthClient.listVitals(PROFILE_ID, null)).thenReturn(vitalsResponse);

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
        when(healthClient.listVitals(PROFILE_ID, null)).thenReturn(emptyVitalsResponse);

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
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString()))
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
        when(householdClient.listCalendarEvents(eq(PROFILE_ID), isNull(), anyString(), anyString()))
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

        when(householdClient.listGoals(PROFILE_ID, null)).thenReturn(goalsResponse);
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

        when(householdClient.listGoals(PROFILE_ID, null)).thenReturn(goalsResponse);
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
    void refreshAll_callsAllSixComputeMethods() throws Exception {
        // Stub all clients with empty-but-valid responses
        when(wealthClient.listAccounts(any(), any(), any()))
                .thenReturn(buildEmptyAccountsResponse());
        when(healthClient.listVitals(any(), any()))
                .thenReturn(MAPPER.readTree("{\"vital_readings\":[]}"));
        when(householdClient.listCalendarEvents(any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"calendar_events\":[]}"));
        when(householdClient.listGoals(any(), any()))
                .thenReturn(MAPPER.readTree("{\"goals\":[]}"));
        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(eq(ADMIN_ID), eq(true))).thenReturn(buildEmptyProfilesResponse());
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        engine.refreshAll(PROFILE_ID);

        // All six SnapshotKey upserts must have fired
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_CATEGORY_VALIDATION), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), anyString());
        // Total: exactly 6 upsert calls
        verify(snapshotRepo, times(6)).upsert(any(), anyString(), anyString());
    }

    // ── computeFamilyNetWorth (ADR-017) ─────────────────────────────────────────

    @Test
    void computeFamilyNetWorth_sumsNetWorthAcrossHouseholdMembers() throws Exception {
        UUID spouseProfileId = UUID.fromString("77777777-0000-0000-0000-000000000002");
        UUID childProfileId = UUID.fromString("77777777-0000-0000-0000-000000000003");

        when(profileClient.getProfile(PROFILE_ID)).thenReturn(buildOwnProfileResponse());
        when(profileClient.listProfiles(eq(ADMIN_ID), eq(true))).thenReturn(buildProfilesResponse(
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

        when(wealthClient.getAccountBalance(eq(selfAccountId), eq(PROFILE_ID.toString())))
                .thenReturn(balanceNode(selfAccountId, 100000.0));
        when(wealthClient.getAccountBalance(eq(spouseAccountId), eq(spouseProfileId.toString())))
                .thenReturn(balanceNode(spouseAccountId, 50000.0));
        when(wealthClient.getAccountBalance(eq(childAccountId), eq(childProfileId.toString())))
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
        when(profileClient.listProfiles(eq(ADMIN_ID), eq(true))).thenReturn(buildProfilesResponse(
                new MemberEntry(PROFILE_ID, "Ketan", "SELF")
        ));

        UUID accountId = UUID.fromString("99999999-0000-0000-0000-000000000001");
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(buildAccountsResponse(accountId));
        when(wealthClient.getAccountBalance(eq(accountId), eq(PROFILE_ID.toString())))
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
        when(profileClient.listProfiles(eq(ADMIN_ID), eq(true))).thenReturn(buildEmptyProfilesResponse());

        engine.computeFamilyNetWorth(PROFILE_ID);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH_FAMILY), payloadCaptor.capture());

        JsonNode payload = MAPPER.readTree(payloadCaptor.getValue());
        assertEquals(0.0, payload.path("family_net_worth").asDouble(), 0.001);
        assertEquals(0, payload.path("member_count").asInt());
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
        when(wealthClient.getAccountBalance(eq(accountId), eq(PROFILE_ID.toString()))).thenReturn(balance);
    }

    private JsonNode buildEmptyAccountsResponse() {
        ObjectNode root = MAPPER.createObjectNode();
        root.set("accounts", MAPPER.createArrayNode());
        return root;
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
}
