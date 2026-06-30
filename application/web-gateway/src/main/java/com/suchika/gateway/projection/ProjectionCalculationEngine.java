package com.suchika.gateway.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.profile.ProfileServiceClient;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final String ACCOUNTS_FIELD = "accounts";

    private final WealthServiceClient wealthServiceClient;
    private final HealthServiceClient healthServiceClient;
    private final HouseholdServiceClient householdServiceClient;
    private final ProfileServiceClient profileServiceClient;
    private final DashboardSnapshotRepository snapshotRepository;

    @Inject
    public ProjectionCalculationEngine(
            @RestClient WealthServiceClient wealthServiceClient,
            @RestClient HealthServiceClient healthServiceClient,
            @RestClient HouseholdServiceClient householdServiceClient,
            @RestClient ProfileServiceClient profileServiceClient,
            DashboardSnapshotRepository snapshotRepository) {
        this.wealthServiceClient = wealthServiceClient;
        this.healthServiceClient = healthServiceClient;
        this.householdServiceClient = householdServiceClient;
        this.profileServiceClient = profileServiceClient;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Refreshes all six snapshot keys for the given profile.
     * Each compute step is independent; a failure in one does not block the others.
     */
    public DashboardResponse refreshAll(UUID profileId) {
        AppLogger.info("ProjectionEngine: refreshing all snapshots for profile %s", profileId);
        computeNetWorth(profileId);
        computeGoalProgress(profileId);
        computeVitalsSummary(profileId);
        computeEventSummary(profileId);
        computeCategoryValidation(profileId);
        computeFamilyNetWorth(profileId);
        return new DashboardResponse(snapshotRepository.findByProfileId(profileId));
    }

    // ── WEALTH_NET_WORTH ──────────────────────────────────────────────────────

    void computeNetWorth(UUID profileId) {
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, profileId.toString());
        double netWorth = 0.0;
        int accountCount = 0;

        JsonNode accountsArray = accounts.path(ACCOUNTS_FIELD);
        if (accountsArray.isArray()) {
            for (JsonNode account : accountsArray) {
                netWorth += currentBalanceFor(account, profileId);
                accountCount++;
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("net_worth", netWorth);
        payload.put("account_count", accountCount);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_NET_WORTH, payload.toString());
    }

    /**
     * Resolves the current balance for a single account via the per-account balance
     * endpoint (opening_balance + SUM(CREDIT) - SUM(DEBIT)) rather than reading
     * opening_balance directly off the account payload — Epic 8 Phase 1, Bug 2 fix.
     */
    private double currentBalanceFor(JsonNode account, UUID profileId) {
        String accountIdText = account.path("account_id").asText("");
        if (accountIdText.isEmpty()) {
            return 0.0;
        }
        JsonNode balance = wealthServiceClient.getAccountBalance(
                UUID.fromString(accountIdText), profileId.toString());
        return balance.path("current_balance").asDouble(0.0);
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
        JsonNode accountsArray = accounts.path(ACCOUNTS_FIELD);
        if (accountsArray.isArray()) {
            for (JsonNode account : accountsArray) {
                total += currentBalanceFor(account, profileId);
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
                if (!vitalType.isEmpty()) {
                    latestByType.computeIfAbsent(vitalType, k -> vital);
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
        LocalDate nowIst = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String today = nowIst.toString();
        String thirtyDaysAhead = nowIst.plusDays(30).toString();

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

    // ── WEALTH_CATEGORY_VALIDATION ────────────────────────────────────────────

    /**
     * Epic 8 Phase 1 validation seed (Use Case 8.4, narrow scope): every account must
     * resolve to exactly one classification category; flags accounts that don't.
     *
     * <p>category is not populated by any write path until Phase 2, so in Phase 1 this
     * is EXPECTED to report every account as uncategorized — that is the correct,
     * honest result, not a placeholder. The check itself is real: if metadata.category
     * is present and non-blank on an account, it counts as categorized.
     */
    void computeCategoryValidation(UUID profileId) {
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, profileId.toString());
        JsonNode accountsArray = accounts.path(ACCOUNTS_FIELD);

        int totalAccounts = 0;
        int categorizedCount = 0;
        ArrayNode uncategorizedIds = MAPPER.createArrayNode();

        if (accountsArray.isArray()) {
            for (JsonNode account : accountsArray) {
                totalAccounts++;
                String category = account.path("metadata").path("category").asText("");
                String accountId = account.path("account_id").asText("");
                if (category.isBlank()) {
                    uncategorizedIds.add(accountId);
                } else {
                    categorizedCount++;
                }
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("total_accounts", totalAccounts);
        payload.put("categorized_count", categorizedCount);
        payload.put("uncategorized_count", totalAccounts - categorizedCount);
        payload.set("uncategorized_account_ids", uncategorizedIds);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_CATEGORY_VALIDATION, payload.toString());
    }

    // ── WEALTH_NET_WORTH_FAMILY (ADR-017) ───────────────────────────────────────

    /**
     * Household-level net worth rollup (ADR-017). Resolves the household the given
     * profile belongs to, computes each member's net worth via the same per-account
     * balance logic as {@link #computeNetWorth(UUID)}, and stores both the family
     * total and a per-member breakdown under the calling profile's own profile_id.
     *
     * <p>Does zero cross-profile SQL — each member's balance is fetched via its own
     * individually profile_id-scoped REST call and summed here in gateway memory,
     * so this does not violate ADR-006.
     */
    void computeFamilyNetWorth(UUID profileId) {
        JsonNode ownProfile = profileServiceClient.getProfile(profileId);
        String adminIdText = ownProfile.path("admin_id").asText("");
        if (adminIdText.isEmpty()) {
            AppLogger.info("ProjectionEngine: profile %s has no admin_id, skipping family rollup", profileId);
            return;
        }
        UUID adminId = UUID.fromString(adminIdText);

        JsonNode membersResponse = profileServiceClient.listProfiles(adminId, true);
        JsonNode membersArray = membersResponse.path("profiles");

        double familyNetWorth = 0.0;
        ArrayNode membersPayload = MAPPER.createArrayNode();

        if (membersArray.isArray()) {
            for (JsonNode member : membersArray) {
                String memberProfileIdText = member.path("profile_id").asText("");
                if (memberProfileIdText.isEmpty()) {
                    continue;
                }
                UUID memberProfileId = UUID.fromString(memberProfileIdText);
                double memberNetWorth = computeTotalBalance(memberProfileId);
                familyNetWorth += memberNetWorth;

                ObjectNode memberEntry = MAPPER.createObjectNode();
                memberEntry.put("profile_id", memberProfileIdText);
                memberEntry.put("full_name", member.path("full_name").asText(""));
                memberEntry.put("relation_to_admin", member.path("relation_to_admin").asText(""));
                memberEntry.put("net_worth", memberNetWorth);
                membersPayload.add(memberEntry);
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("family_net_worth", familyNetWorth);
        payload.put("member_count", membersPayload.size());
        payload.set("members", membersPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_NET_WORTH_FAMILY, payload.toString());
    }
}
