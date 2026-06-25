package com.suchika.gateway.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
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

    @Mock
    WealthServiceClient wealthClient;

    @Mock
    HealthServiceClient healthClient;

    @Mock
    HouseholdServiceClient householdClient;

    @Mock
    DashboardSnapshotRepository snapshotRepo;

    ProjectionCalculationEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = new ProjectionCalculationEngine(wealthClient, healthClient, householdClient, snapshotRepo);
    }

    // ── computeNetWorth ───────────────────────────────────────────────────────

    @Test
    void computeNetWorth_sumsAccountBalances() throws Exception {
        JsonNode accountsResponse = buildAccountsResponse(1000.0, 500.0);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);

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
        JsonNode accountsResponse = buildAccountsResponse(600.0);

        when(householdClient.listGoals(PROFILE_ID, null)).thenReturn(goalsResponse);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
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
        JsonNode accountsResponse = buildAccountsResponse(1000.0);

        when(householdClient.listGoals(PROFILE_ID, null)).thenReturn(goalsResponse);
        when(wealthClient.listAccounts(isNull(), eq(true), eq(PROFILE_ID.toString())))
                .thenReturn(accountsResponse);
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

    // ── refreshAll ───────────────────────────────────────────────────────────

    @Test
    void refreshAll_callsAllFourComputeMethods() throws Exception {
        // Stub all clients with empty-but-valid responses
        when(wealthClient.listAccounts(any(), any(), any()))
                .thenReturn(buildEmptyAccountsResponse());
        when(healthClient.listVitals(any(), any()))
                .thenReturn(MAPPER.readTree("{\"vital_readings\":[]}"));
        when(householdClient.listCalendarEvents(any(), any(), any(), any()))
                .thenReturn(MAPPER.readTree("{\"calendar_events\":[]}"));
        when(householdClient.listGoals(any(), any()))
                .thenReturn(MAPPER.readTree("{\"goals\":[]}"));
        when(snapshotRepo.findByProfileId(PROFILE_ID)).thenReturn(List.of());

        engine.refreshAll(PROFILE_ID);

        // All four SnapshotKey upserts must have fired
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_NET_WORTH), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.WEALTH_GOAL_PROGRESS), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HEALTH_VITALS_SUMMARY), anyString());
        verify(snapshotRepo).upsert(eq(PROFILE_ID), eq(SnapshotKey.HOUSEHOLD_EVENT_SUMMARY), anyString());
        // Total: exactly 4 upsert calls
        verify(snapshotRepo, times(4)).upsert(any(), anyString(), anyString());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JsonNode buildAccountsResponse(double... balances) {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode accounts = MAPPER.createArrayNode();
        for (double balance : balances) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("opening_balance", balance);
            accounts.add(account);
        }
        root.set("accounts", accounts);
        return root;
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
}
