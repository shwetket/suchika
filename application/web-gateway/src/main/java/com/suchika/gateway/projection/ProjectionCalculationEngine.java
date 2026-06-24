package com.suchika.gateway.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Computes cross-domain metrics and persists them to projections.dashboard_snapshot.
 *
 * <p>One public entry point: {@link #refreshAll(UUID)}.
 * Each private compute method is responsible for one snapshot key.
 * Extend by adding a new private method + calling it from refreshAll.
 */
@ApplicationScoped
public class ProjectionCalculationEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String VITAL_TYPE_KEY = "vital_type";

    private final WealthServiceClient wealthServiceClient;
    private final HealthServiceClient healthServiceClient;
    private final HouseholdServiceClient householdServiceClient;
    private final DashboardSnapshotRepository snapshotRepository;

    @Inject
    public ProjectionCalculationEngine(
            @RestClient WealthServiceClient wealthServiceClient,
            @RestClient HealthServiceClient healthServiceClient,
            @RestClient HouseholdServiceClient householdServiceClient,
            DashboardSnapshotRepository snapshotRepository) {
        this.wealthServiceClient = wealthServiceClient;
        this.healthServiceClient = healthServiceClient;
        this.householdServiceClient = householdServiceClient;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Refreshes all four snapshot keys for the given profile.
     * Each compute step is independent; a failure in one does not block the others.
     */
    public DashboardResponse refreshAll(UUID profileId) {
        AppLogger.info("ProjectionEngine: refreshing all snapshots for profile %s", profileId);
        computeNetWorth(profileId);
        computeGoalProgress(profileId);
        computeVitalsSummary(profileId);
        computeEventSummary(profileId);
        return new DashboardResponse(snapshotRepository.findByProfileId(profileId));
    }

    // ── WEALTH_NET_WORTH ──────────────────────────────────────────────────────

    void computeNetWorth(UUID profileId) {
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, profileId.toString());
        double netWorth = 0.0;
        int accountCount = 0;

        JsonNode accountsArray = accounts.path("accounts");
        if (accountsArray.isArray()) {
            for (JsonNode account : accountsArray) {
                JsonNode balanceNode = account.path("opening_balance");
                if (!balanceNode.isMissingNode()) {
                    netWorth += balanceNode.asDouble(0.0);
                }
                accountCount++;
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("net_worth", netWorth);
        payload.put("account_count", accountCount);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_NET_WORTH, payload.toString());
    }

    // ── WEALTH_GOAL_PROGRESS ──────────────────────────────────────────────────

    void computeGoalProgress(UUID profileId) {
        JsonNode goalsResponse = householdServiceClient.listGoals(profileId, null);
        JsonNode goalsArray = goalsResponse.path("goals");

        double totalBalance = computeTotalBalance(profileId);

        ArrayNode goalsPayload = MAPPER.createArrayNode();
        if (goalsArray.isArray()) {
            for (JsonNode goal : goalsArray) {
                String goalId = goal.path("id").asText("");
                String goalName = goal.path("goal_name").asText("");
                double targetAmount = goal.path("target_amount").asDouble(0.0);
                double currentAmount = Math.min(totalBalance, targetAmount);
                double progressPercent = targetAmount > 0
                        ? Math.min(100.0, currentAmount / targetAmount * 100.0)
                        : 0.0;

                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("id", goalId);
                entry.put("goal_name", goalName);
                entry.put("target_amount", targetAmount);
                entry.put("current_amount", currentAmount);
                entry.put("progress_percent", progressPercent);
                goalsPayload.add(entry);

                if (!goalId.isEmpty()) {
                    writeBackGoalCurrentAmount(UUID.fromString(goalId), currentAmount);
                }
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("goals", goalsPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_GOAL_PROGRESS, payload.toString());
    }

    private double computeTotalBalance(UUID profileId) {
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, profileId.toString());
        double total = 0.0;
        JsonNode accountsArray = accounts.path("accounts");
        if (accountsArray.isArray()) {
            for (JsonNode account : accountsArray) {
                total += account.path("opening_balance").asDouble(0.0);
            }
        }
        return total;
    }

    private void writeBackGoalCurrentAmount(UUID goalId, double currentAmount) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("current_amount", currentAmount);
        householdServiceClient.updateGoalCurrentAmount(goalId, body);
    }

    // ── HEALTH_VITALS_SUMMARY ─────────────────────────────────────────────────

    void computeVitalsSummary(UUID profileId) {
        JsonNode vitalsResponse = healthServiceClient.listVitals(profileId, null);
        JsonNode vitalsArray = vitalsResponse.path("vital_readings");

        // Keep only the latest reading per vital_type (readings assumed newest-first)
        java.util.Map<String, JsonNode> latestByType = new java.util.LinkedHashMap<>();
        if (vitalsArray.isArray()) {
            for (JsonNode vital : vitalsArray) {
                String vitalType = vital.path(VITAL_TYPE_KEY).asText("");
                if (!vitalType.isEmpty() && !latestByType.containsKey(vitalType)) {
                    latestByType.put(vitalType, vital);
                }
            }
        }

        ArrayNode vitalsPayload = MAPPER.createArrayNode();
        for (JsonNode vital : latestByType.values()) {
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put(VITAL_TYPE_KEY, vital.path(VITAL_TYPE_KEY).asText(""));
            entry.put("value_primary", vital.path("value_primary").asDouble(0.0));
            entry.put("value_secondary", vital.path("value_secondary").asDouble(0.0));
            entry.put("unit", vital.path("unit").asText(""));
            entry.put("reading_date", vital.path("reading_date").asText(""));
            vitalsPayload.add(entry);
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("vitals", vitalsPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.HEALTH_VITALS_SUMMARY, payload.toString());
    }

    // ── HOUSEHOLD_EVENT_SUMMARY ───────────────────────────────────────────────

    void computeEventSummary(UUID profileId) {
        String today = LocalDate.now().toString();
        String thirtyDaysAhead = LocalDate.now().plusDays(30).toString();

        JsonNode eventsResponse = householdServiceClient.listCalendarEvents(
                profileId, null, today, thirtyDaysAhead);
        JsonNode eventsArray = eventsResponse.path("calendar_events");

        ArrayNode eventsPayload = MAPPER.createArrayNode();
        int upcomingCount = 0;
        if (eventsArray.isArray()) {
            for (JsonNode event : eventsArray) {
                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("id", event.path("id").asText(""));
                entry.put("title", event.path("title").asText(""));
                entry.put("start_date", event.path("start_date").asText(""));
                eventsPayload.add(entry);
                upcomingCount++;
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("upcoming_count", upcomingCount);
        payload.set("events", eventsPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.HOUSEHOLD_EVENT_SUMMARY, payload.toString());
    }
}
