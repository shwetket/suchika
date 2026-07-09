package com.suchika.gateway.projection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.profile.ProfileServiceClient;
import com.suchika.gateway.wealth.ExpiryDateUtil;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
    private static final String ACCOUNT_TYPE_FIELD = "account_type";
    private static final String ACCOUNT_ID_FIELD = "account_id";
    private static final String ACCOUNT_NAME_FIELD = "account_name";
    private static final String CURRENT_BALANCE_FIELD = "current_balance";
    private static final String PROFILE_ID_FIELD = "profile_id";
    private static final String FULL_NAME_FIELD = "full_name";
    private static final String MEMBERS_FIELD = "members";
    private static final String MEMBER_COUNT_FIELD = "member_count";
    private static final String LOAN_TYPE_HOME = "HOME_LOAN";
    private static final String LOAN_TYPE_CAR = "CAR_LOAN";
    private static final String LOAN_TYPE_PERSONAL = "PERSONAL_LOAN";
    private static final String INV_TYPE_MUTUAL_FUND = "MUTUAL_FUND";
    private static final String INV_TYPE_NPS = "NPS";
    private static final String INV_TYPE_PPF = "PPF";
    private static final String METADATA_FIELD = "metadata";
    private static final String PROFILES_FIELD = "profiles";
    private static final String OUTSTANDING_BALANCE_FIELD = "outstanding_balance";
    private static final String MONTHLY_EMI_FIELD = "monthly_emi";

    // Phase 4 — goals engine field name constants (Sonar S1192: 3+ uses → named constant)
    private static final String STATUS_FIELD = "status";
    private static final String GOAL_ID_FIELD = "goal_id";
    private static final String GOAL_NAME_FIELD = "goal_name";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String CURRENT_VALUE_FIELD = "current_value";
    private static final String TARGET_VALUE_FIELD = "target_value";
    private static final String UNIT_FIELD = "unit";
    private static final String CHECK_ID_FIELD = "check_id";
    private static final String MESSAGE_FIELD = "message";
    private static final String SEVERITY_FIELD = "severity";
    private static final String OVERALL_STATUS_FIELD = "overall_status";
    private static final String CHECKS_FIELD = "checks";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARNING = "WARNING";
    private static final String STATUS_ACHIEVED = "ACHIEVED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String POLICY_SETTINGS_FIELD = "policy_settings";
    private static final String GOALS_FIELD = "goals";
    private static final String TOTAL_MONTHLY_EMI_FIELD = "total_monthly_emi";
    private static final String TIER_LIQUID = "LIQUID";
    private static final String TIER_SEMI_LIQUID = "SEMI_LIQUID";

    // Phase 3 (v0.5) — Consolidated Action Center field name constants
    private static final String ASSET_ID_FIELD = "asset_id";
    private static final String ASSET_NAME_FIELD = "asset_name";
    private static final String PHYSICAL_ASSETS_FIELD = "physical_assets";
    private static final String VEHICLE_ASSET_TYPE = "VEHICLE";
    private static final String VITAL_READINGS_FIELD = "vital_readings";
    private static final String EVENTS_FIELD = "calendar_events";
    private static final String UPCOMING_EVENTS_FIELD = "upcoming_events";
    private static final String VEHICLE_COMPLIANCE_FIELD = "vehicle_compliance";
    private static final String BIOMETRIC_STREAK_GAPS_FIELD = "biometric_streak_gaps";
    private static final String LAST_READING_DATE_FIELD = "last_reading_date";
    private static final String READING_DATE_FIELD = "reading_date";
    private static final String TITLE_FIELD = "title";
    private static final String START_DATE_FIELD = "start_date";
    private static final int STREAK_GAP_THRESHOLD_DAYS = 30;
    private static final int ACTION_CENTER_EVENT_LOOKAHEAD_DAYS = 30;
    // Pre-v1.0 pagination pass (Q54): GET /v1/vitals now defaults to 50 rows per page.
    // These two internal aggregations only need the latest reading per vital_type (10
    // types total, newest-first), so the system's own page-size ceiling is used to keep
    // this call effectively unbounded for realistic reading volumes without introducing
    // an unpaginated code path back into the contract.
    private static final int VITALS_AGGREGATION_FETCH_SIZE = 200;
    private static final List<String> STREAK_TRACKED_VITAL_TYPES =
            List.of("WEIGHT", "BLOOD_PRESSURE", "BLOOD_SUGAR_FASTING");

    private static final double DEFAULT_DEBT_CROSSOVER_THRESHOLD = 50.0;
    private static final double DEFAULT_MONTHLY_BUDGET_CAP = 0.0;
    private static final double DEFAULT_FREEDOM_RUNWAY_MONTHS = 6.0;
    private static final double DEFAULT_INSURANCE_MULTIPLE = 10.0;
    private static final double DEFAULT_YEAR_ONE_ANNUAL_TARGET = 1_000_000.0;

    // Epic 8 Phase 3 — annual growth rates injected via config; field initializer provides
    // the same default so plain `new` in unit tests produces correct expected values.
    @ConfigProperty(name = "app.wealth.growth.rate.MUTUAL_FUND", defaultValue = "0.12")
    double mutualFundRate = 0.12;

    @ConfigProperty(name = "app.wealth.growth.rate.NPS", defaultValue = "0.10")
    double npsRate = 0.10;

    @ConfigProperty(name = "app.wealth.growth.rate.PPF", defaultValue = "0.071")
    double ppfRate = 0.071;

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
     * Refreshes all eleven snapshot keys for the given profile.
     * Each compute step is independent; a failure in one does not block the others
     * (Bug 3 fix — see Epic 8 Phase 4 / OpenQuestions.md Q4).
     *
     * <p>Step ordering matters: Phase 4 steps (11-12) must run after Phase 3 steps
     * (7-9) because computeFormulaGoals and computeValidation read the Phase 3
     * snapshot payloads from the repository rather than re-calling domain services.
     *
     * <p>Steps:
     * 1  computeNetWorth              — WEALTH_NET_WORTH
     * 2  computeGoalProgress          — WEALTH_GOAL_PROGRESS
     * 3  computeVitalsSummary         — HEALTH_VITALS_SUMMARY
     * 4  computeEventSummary          — HOUSEHOLD_EVENT_SUMMARY
     * 5  computeCategoryValidation    — WEALTH_CATEGORY_VALIDATION
     * 6  computeFamilyNetWorth        — WEALTH_NET_WORTH_FAMILY
     * 7  computeEmiTracking           — WEALTH_EMI_TRACKING_FAMILY
     * 8  computeLiquidityTiers        — WEALTH_LIQUIDITY_TIERS_FAMILY
     * 9  computeGrowthProjection      — WEALTH_GROWTH_PROJECTION_FAMILY
     * 10 computeFormulaGoals          — WEALTH_FORMULA_GOALS_FAMILY
     * 11 computeValidation            — WEALTH_VALIDATION_REPORT_FAMILY
     * 12 computeActionCenterAlerts    — ACTION_CENTER_ALERTS_FAMILY
     */
    public DashboardResponse refreshAll(UUID profileId) {
        AppLogger.info("ProjectionEngine: refreshing all snapshots for profile %s", profileId);
        runStep("computeNetWorth", profileId, this::computeNetWorth);
        runStep("computeGoalProgress", profileId, this::computeGoalProgress);
        runStep("computeVitalsSummary", profileId, this::computeVitalsSummary);
        runStep("computeEventSummary", profileId, this::computeEventSummary);
        runStep("computeCategoryValidation", profileId, this::computeCategoryValidation);
        runStep("computeFamilyNetWorth", profileId, this::computeFamilyNetWorth);
        runStep("computeEmiTracking", profileId, this::computeEmiTracking);
        runStep("computeLiquidityTiers", profileId, this::computeLiquidityTiers);
        runStep("computeGrowthProjection", profileId, this::computeGrowthProjection);
        runStep("computeFormulaGoals", profileId, this::computeFormulaGoals);
        runStep("computeValidation", profileId, this::computeValidation);
        runStep("computeActionCenterAlerts", profileId,
                id -> computeActionCenterAlerts(id, LocalDate.now(ZoneId.of("Asia/Kolkata"))));
        return new DashboardResponse(snapshotRepository.findByProfileId(profileId));
    }

    private void runStep(String stepName, UUID profileId, Consumer<UUID> step) {
        try {
            step.accept(profileId);
        } catch (RuntimeException e) {
            AppLogger.error("ProjectionEngine: step %s failed for profile %s, skipping", e, stepName, profileId);
        }
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

        double physicalAssetValue = physicalAssetValueFor(profileId);
        netWorth += physicalAssetValue;

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("net_worth", netWorth);
        payload.put("account_count", accountCount);
        payload.put("physical_asset_value", physicalAssetValue);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_NET_WORTH, payload.toString());
    }

    /**
     * Sums current_value across all active physical assets (real estate, gold, vehicles
     * with a set value, etc.) for {@link #computeNetWorth(UUID)} — closes the v1.0
     * net-worth-model gap where real estate (85.7% of the reference household's net
     * worth) and gold had no path into WEALTH_NET_WORTH at all.
     *
     * <p><b>Deliberately NOT wired into {@link #computeTotalBalance(UUID)}</b> (which
     * backs {@link #computeGoalProgress(UUID)}) — that method feeds goals like Freedom
     * Runway that are specifically about liquid/liquid-adjacent capital per
     * Financial_Data.md's own liquidity tiering; blending in illiquid real estate/gold
     * value there would silently distort those goals. This is a deliberate scope
     * boundary, not an oversight.
     */
    private double physicalAssetValueFor(UUID profileId) {
        JsonNode assetsResponse = wealthServiceClient.listPhysicalAssets(null, true, profileId.toString(), null, null);
        JsonNode assetsArray = assetsResponse.path(PHYSICAL_ASSETS_FIELD);
        double total = 0.0;
        if (assetsArray.isArray()) {
            for (JsonNode asset : assetsArray) {
                total += asset.path(CURRENT_VALUE_FIELD).asDouble(0.0);
            }
        }
        return total;
    }

    /**
     * Resolves the current balance for a single account via the per-account balance
     * endpoint (opening_balance + SUM(CREDIT) - SUM(DEBIT)) rather than reading
     * opening_balance directly off the account payload — Epic 8 Phase 1, Bug 2 fix.
     */
    private double currentBalanceFor(JsonNode account, UUID profileId) {
        String accountIdText = account.path(ACCOUNT_ID_FIELD).asText("");
        if (accountIdText.isEmpty()) {
            return 0.0;
        }
        JsonNode balance = wealthServiceClient.getAccountBalance(
                UUID.fromString(accountIdText), profileId.toString());
        return balance.path(CURRENT_BALANCE_FIELD).asDouble(0.0);
    }

    // ── WEALTH_GOAL_PROGRESS ──────────────────────────────────────────────────

    void computeGoalProgress(UUID profileId) {
        JsonNode goalsResponse = householdServiceClient.listGoals(profileId, null, null, null);
        JsonNode goalsArray = goalsResponse.path(GOALS_FIELD);

        double totalBalance = computeTotalBalance(profileId);

        ArrayNode goalsPayload = MAPPER.createArrayNode();
        if (goalsArray.isArray()) {
            for (JsonNode goal : goalsArray) {
                String goalId = goal.path("id").asText("");
                String goalName = goal.path(GOAL_NAME_FIELD).asText("");
                double targetAmount = goal.path("target_amount").asDouble(0.0);
                double currentAmount = Math.min(totalBalance, targetAmount);
                double progressPercent = targetAmount > 0
                        ? Math.min(100.0, currentAmount / targetAmount * 100.0)
                        : 0.0;

                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("id", goalId);
                entry.put(GOAL_NAME_FIELD, goalName);
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
        payload.set(GOALS_FIELD, goalsPayload);
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
        JsonNode vitalsResponse = healthServiceClient.listVitals(profileId, null, 0, VITALS_AGGREGATION_FETCH_SIZE);
        JsonNode vitalsArray = vitalsResponse.path(VITAL_READINGS_FIELD);

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
            entry.put(READING_DATE_FIELD, vital.path(READING_DATE_FIELD).asText(""));
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
                profileId, null, today, thirtyDaysAhead, null, null);
        JsonNode eventsArray = eventsResponse.path(EVENTS_FIELD);

        ArrayNode eventsPayload = MAPPER.createArrayNode();
        int upcomingCount = 0;
        if (eventsArray.isArray()) {
            for (JsonNode event : eventsArray) {
                ObjectNode entry = MAPPER.createObjectNode();
                entry.put("id", event.path("id").asText(""));
                entry.put(TITLE_FIELD, event.path(TITLE_FIELD).asText(""));
                entry.put(START_DATE_FIELD, event.path(START_DATE_FIELD).asText(""));
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
                String category = account.path(METADATA_FIELD).path("category").asText("");
                String accountId = account.path(ACCOUNT_ID_FIELD).asText("");
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

    // ── WEALTH_NET_WORTH_FAMILY (ADR-017) ─────────────────────────────────────

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
        JsonNode membersArray = membersResponse.path(PROFILES_FIELD);

        double familyNetWorth = 0.0;
        ArrayNode membersPayload = MAPPER.createArrayNode();

        if (membersArray.isArray()) {
            for (JsonNode member : membersArray) {
                String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
                if (memberProfileIdText.isEmpty()) {
                    continue;
                }
                UUID memberProfileId = UUID.fromString(memberProfileIdText);
                double memberNetWorth = computeTotalBalance(memberProfileId);
                familyNetWorth += memberNetWorth;

                ObjectNode memberEntry = MAPPER.createObjectNode();
                memberEntry.put(PROFILE_ID_FIELD, memberProfileIdText);
                memberEntry.put(FULL_NAME_FIELD, member.path(FULL_NAME_FIELD).asText(""));
                memberEntry.put("relation_to_admin", member.path("relation_to_admin").asText(""));
                memberEntry.put("net_worth", memberNetWorth);
                membersPayload.add(memberEntry);
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("family_net_worth", familyNetWorth);
        payload.put(MEMBER_COUNT_FIELD, membersPayload.size());
        payload.set(MEMBERS_FIELD, membersPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_NET_WORTH_FAMILY, payload.toString());
    }

    // ── WEALTH_EMI_TRACKING_FAMILY (ADR-017) ──────────────────────────────────

    /**
     * Epic 8 Phase 3 — Loan EMI tracking with offset arbitrage (Use Case 8.2).
     * Aggregates across all household members. If amortization data is unavailable for
     * an account (metadata not yet set), that account is silently skipped.
     */
    void computeEmiTracking(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }
        JsonNode membersArray = profileServiceClient.listProfiles(adminId, true).path(PROFILES_FIELD);

        double totalOutstanding = 0.0;
        double totalEmi = 0.0;
        double totalInterestSaved = 0.0;
        ArrayNode membersPayload = MAPPER.createArrayNode();
        int memberCount = 0;

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            double[] memberTotals = new double[3]; // [outstanding, emi, interestSaved]
            ArrayNode loansPayload = buildMemberLoanEntries(memberProfileIdText, memberTotals);

            ObjectNode memberEntry = MAPPER.createObjectNode();
            memberEntry.put(PROFILE_ID_FIELD, memberProfileIdText);
            memberEntry.put(FULL_NAME_FIELD, member.path(FULL_NAME_FIELD).asText(""));
            memberEntry.set("loans", loansPayload);
            membersPayload.add(memberEntry);

            totalOutstanding += memberTotals[0];
            totalEmi += memberTotals[1];
            totalInterestSaved += memberTotals[2];
            memberCount++;
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("total_outstanding_balance", totalOutstanding);
        payload.put(TOTAL_MONTHLY_EMI_FIELD, totalEmi);
        payload.put("total_monthly_interest_saved", totalInterestSaved);
        payload.put(MEMBER_COUNT_FIELD, memberCount);
        payload.set(MEMBERS_FIELD, membersPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_EMI_TRACKING_FAMILY, payload.toString());
    }

    private ArrayNode buildMemberLoanEntries(String memberProfileIdText, double[] totals) {
        ArrayNode loansPayload = MAPPER.createArrayNode();
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            ObjectNode loanEntry = buildLoanEntry(account, memberProfileIdText);
            if (loanEntry == null) {
                continue;
            }
            loansPayload.add(loanEntry);
            totals[0] += loanEntry.path(OUTSTANDING_BALANCE_FIELD).asDouble(0.0);
            totals[1] += loanEntry.path(MONTHLY_EMI_FIELD).asDouble(0.0);
            totals[2] += loanEntry.path("monthly_interest_saved").asDouble(0.0);
        }
        return loansPayload;
    }

    private ObjectNode buildLoanEntry(JsonNode account, String memberProfileIdText) {
        if (!isLoanType(account.path(ACCOUNT_TYPE_FIELD).asText(""))) {
            return null;
        }
        String accountIdText = account.path(ACCOUNT_ID_FIELD).asText("");
        if (accountIdText.isEmpty()) {
            return null;
        }
        JsonNode amortization;
        try {
            amortization = wealthServiceClient.getAmortization(UUID.fromString(accountIdText), memberProfileIdText);
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: amortization unavailable for account %s, skipping", accountIdText);
            return null;
        }

        double outstanding = amortization.path(OUTSTANDING_BALANCE_FIELD).asDouble(0.0);
        double monthlyEmi = amortization.path(MONTHLY_EMI_FIELD).asDouble(0.0);
        int remainingMonths = amortization.path("remaining_months").asInt(0);
        double annualRate = amortization.path("interest_rate").asDouble(0.0);
        double monthlyInterestSaved = computeOffsetSavings(account, memberProfileIdText, annualRate);

        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(ACCOUNT_ID_FIELD, accountIdText);
        entry.put(ACCOUNT_NAME_FIELD, account.path(ACCOUNT_NAME_FIELD).asText(""));
        entry.put(OUTSTANDING_BALANCE_FIELD, outstanding);
        entry.put(MONTHLY_EMI_FIELD, monthlyEmi);
        entry.put("remaining_months", remainingMonths);
        entry.put("monthly_interest_saved", monthlyInterestSaved);
        return entry;
    }

    private double computeOffsetSavings(JsonNode account, String memberProfileIdText, double annualRate) {
        String offsetAccountId = account.path(METADATA_FIELD).path("linked_offset_account_id").asText("");
        if (offsetAccountId.isEmpty()) {
            return 0.0;
        }
        try {
            JsonNode offsetBalance = wealthServiceClient.getAccountBalance(
                    UUID.fromString(offsetAccountId), memberProfileIdText);
            double offsetBal = offsetBalance.path(CURRENT_BALANCE_FIELD).asDouble(0.0);
            return offsetBal * (annualRate / 12.0 / 100.0);
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: offset balance unavailable for %s, skipping arbitrage", offsetAccountId);
            return 0.0;
        }
    }

    private boolean isLoanType(String accountType) {
        return LOAN_TYPE_HOME.equals(accountType)
                || LOAN_TYPE_CAR.equals(accountType)
                || LOAN_TYPE_PERSONAL.equals(accountType);
    }

    // ── WEALTH_LIQUIDITY_TIERS_FAMILY (ADR-017) ────────────────────────────────

    /**
     * Epic 8 Phase 3 — Liquidity tiering across all household members (Use Case 8.1).
     * Groups account balances by metadata.liquidity_tier. Unknown/blank tier goes to UNCLASSIFIED.
     */
    void computeLiquidityTiers(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }
        JsonNode membersArray = profileServiceClient.listProfiles(adminId, true).path(PROFILES_FIELD);

        double liquidTotal = 0.0;
        double semiLiquidTotal = 0.0;
        double illiquidTotal = 0.0;
        double lockedTotal = 0.0;
        double unclassifiedTotal = 0.0;
        int memberCount = 0;

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            UUID memberProfileId = UUID.fromString(memberProfileIdText);
            double[] tierTotals = accumulateTiersForMember(memberProfileIdText, memberProfileId);
            liquidTotal += tierTotals[0];
            semiLiquidTotal += tierTotals[1];
            illiquidTotal += tierTotals[2];
            lockedTotal += tierTotals[3];
            unclassifiedTotal += tierTotals[4];
            memberCount++;
        }

        double grandTotal = liquidTotal + semiLiquidTotal + illiquidTotal + lockedTotal + unclassifiedTotal;

        ObjectNode tiers = MAPPER.createObjectNode();
        tiers.put(TIER_LIQUID, liquidTotal);
        tiers.put(TIER_SEMI_LIQUID, semiLiquidTotal);
        tiers.put("ILLIQUID", illiquidTotal);
        tiers.put("LOCKED", lockedTotal);
        tiers.put("UNCLASSIFIED", unclassifiedTotal);

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set("tiers", tiers);
        payload.put("total", grandTotal);
        payload.put(MEMBER_COUNT_FIELD, memberCount);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY, payload.toString());
    }

    private double[] accumulateTiersForMember(String memberProfileIdText, UUID memberProfileId) {
        double[] totals = new double[5]; // [LIQUID, SEMI_LIQUID, ILLIQUID, LOCKED, UNCLASSIFIED]
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            double balance = currentBalanceFor(account, memberProfileId);
            String tier = account.path(METADATA_FIELD).path("liquidity_tier").asText("").trim();
            switch (tier) {
                case TIER_LIQUID -> totals[0] += balance;
                case TIER_SEMI_LIQUID -> totals[1] += balance;
                case "ILLIQUID" -> totals[2] += balance;
                case "LOCKED" -> totals[3] += balance;
                default -> totals[4] += balance;
            }
        }
        return totals;
    }

    // ── WEALTH_GROWTH_PROJECTION_FAMILY (ADR-017) ─────────────────────────────

    /**
     * Epic 8 Phase 3 — 5yr/10yr growth projections for investment accounts (Use Case 8.1).
     * Applies configured annual rates: MUTUAL_FUND=12%, NPS=10%, PPF=7.1%.
     */
    void computeGrowthProjection(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }
        JsonNode membersArray = profileServiceClient.listProfiles(adminId, true).path(PROFILES_FIELD);

        ArrayNode projectionsPayload = MAPPER.createArrayNode();

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            UUID memberProfileId = UUID.fromString(memberProfileIdText);
            addMemberProjections(memberProfileIdText, memberProfileId, projectionsPayload);
        }

        double totalCurrentValue = 0.0;
        double totalProjected5yr = 0.0;
        double totalProjected10yr = 0.0;
        for (JsonNode proj : projectionsPayload) {
            totalCurrentValue += proj.path(CURRENT_BALANCE_FIELD).asDouble(0.0);
            totalProjected5yr += proj.path("projected_5yr").asDouble(0.0);
            totalProjected10yr += proj.path("projected_10yr").asDouble(0.0);
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("total_current_value", totalCurrentValue);
        payload.put("total_projected_5yr", totalProjected5yr);
        payload.put("total_projected_10yr", totalProjected10yr);
        payload.set("projections", projectionsPayload);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY, payload.toString());
    }

    private void addMemberProjections(String memberProfileIdText, UUID memberProfileId, ArrayNode projectionsPayload) {
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            ObjectNode projEntry = buildProjectionEntry(account, memberProfileId);
            if (projEntry != null) {
                projectionsPayload.add(projEntry);
            }
        }
    }

    private ObjectNode buildProjectionEntry(JsonNode account, UUID memberProfileId) {
        String accountType = account.path(ACCOUNT_TYPE_FIELD).asText("");
        double rate = growthRateFor(accountType);
        if (rate < 0) {
            return null;
        }
        String accountIdText = account.path(ACCOUNT_ID_FIELD).asText("");
        if (accountIdText.isEmpty()) {
            return null;
        }
        double currentBalance = currentBalanceFor(account, memberProfileId);
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(ACCOUNT_ID_FIELD, accountIdText);
        entry.put(ACCOUNT_NAME_FIELD, account.path(ACCOUNT_NAME_FIELD).asText(""));
        entry.put(ACCOUNT_TYPE_FIELD, accountType);
        entry.put(CURRENT_BALANCE_FIELD, currentBalance);
        entry.put("growth_rate_annual", rate);
        entry.put("projected_5yr", currentBalance * Math.pow(1.0 + rate, 5));
        entry.put("projected_10yr", currentBalance * Math.pow(1.0 + rate, 10));
        return entry;
    }

    /**
     * Returns the configured annual growth rate for an investment account type,
     * or -1 if the account type is not a tracked investment type.
     */
    private double growthRateFor(String accountType) {
        if (INV_TYPE_MUTUAL_FUND.equals(accountType)) {
            return mutualFundRate;
        } else if (INV_TYPE_NPS.equals(accountType)) {
            return npsRate;
        } else if (INV_TYPE_PPF.equals(accountType)) {
            return ppfRate;
        }
        return -1.0;
    }

    // ── WEALTH_FORMULA_GOALS_FAMILY (Epic 8 Phase 4) ──────────────────────────

    /**
     * Epic 8 Phase 4 — five hardcoded formula goals engine (Use Case 8.5).
     * Reads Phase 3 snapshot payloads already stored in the repository — no new
     * domain REST calls needed. Policy thresholds are read from admin.policy_settings.
     *
     * <p>Goal 1 — Debt Crossover: totalMonthlyEmi / familyNetWorth * 100 &lt; threshold.
     * Goal 2 — 30-70 Target: LIQUID tier / total &gt;= 30%.
     * Goal 3 — Freedom Runway: (LIQUID + SEMI_LIQUID) / monthlyBudgetCap &gt;= targetMonths.
     * Goal 4 — Insurance Free: totalInvestmentValue &gt;= annualIncome * insuranceMultiple.
     * Goal 5 — Year One: familyNetWorth &gt;= yearOneAnnualTarget.
     */
    void computeFormulaGoals(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }

        // Read policy settings from admin
        JsonNode policySettings = resolveAdminPolicySettings(adminId);

        // Read Phase 3 snapshot payloads from the repository
        java.util.Map<String, JsonNode> snapshots = loadSnapshotsAsMap(profileId);

        JsonNode netWorthPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_NET_WORTH_FAMILY, MAPPER.createObjectNode());
        JsonNode emiPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY, MAPPER.createObjectNode());
        JsonNode liquidityPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY, MAPPER.createObjectNode());
        JsonNode growthPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY, MAPPER.createObjectNode());

        double familyNetWorth = netWorthPayload.path("family_net_worth").asDouble(0.0);
        double totalMonthlyEmi = emiPayload.path(TOTAL_MONTHLY_EMI_FIELD).asDouble(0.0);
        JsonNode tiers = liquidityPayload.path("tiers");
        double liquidBalance = tiers.path(TIER_LIQUID).asDouble(0.0);
        double semiLiquidBalance = tiers.path(TIER_SEMI_LIQUID).asDouble(0.0);
        double totalLiquidity = liquidityPayload.path("total").asDouble(0.0);
        double totalInvestmentValue = growthPayload.path("total_current_value").asDouble(0.0);

        // Policy values with defaults
        double debtThreshold = policySettings.path("debt_crossover_threshold_percent").asDouble(DEFAULT_DEBT_CROSSOVER_THRESHOLD);
        double monthlyBudgetCap = policySettings.path("monthly_budget_cap").asDouble(DEFAULT_MONTHLY_BUDGET_CAP);
        double freedomRunwayMonths = policySettings.path("freedom_runway_months").asDouble(DEFAULT_FREEDOM_RUNWAY_MONTHS);
        double insuranceMultiple = policySettings.path("insurance_multiple").asDouble(DEFAULT_INSURANCE_MULTIPLE);
        double yearOneTarget = policySettings.path("year_one_annual_target").asDouble(DEFAULT_YEAR_ONE_ANNUAL_TARGET);

        ArrayNode goalsArray = MAPPER.createArrayNode();
        int achievedCount = 0;

        // Goal 1 — Debt Crossover
        double debtPercent = familyNetWorth > 0 ? totalMonthlyEmi / familyNetWorth * 100.0 : 0.0;
        boolean debtAchieved = debtPercent < debtThreshold;
        goalsArray.add(buildGoalEntry(
                "DEBT_CROSSOVER",
                "Debt Crossover",
                debtAchieved,
                "Monthly EMI below " + (int) debtThreshold + "% of net worth",
                debtPercent,
                debtThreshold,
                "percent"));
        if (debtAchieved) achievedCount++;

        // Goal 2 — 30-70 Target
        double liquidPercent = totalLiquidity > 0 ? liquidBalance / totalLiquidity * 100.0 : 0.0;
        double growthPercent = totalLiquidity > 0 ? totalInvestmentValue / totalLiquidity * 100.0 : 0.0;
        boolean thirtySeventyAchieved = liquidPercent >= 30.0;
        ObjectNode thirtySeventyEntry = buildGoalEntry(
                "THIRTY_SEVENTY_TARGET",
                "30-70 Target",
                thirtySeventyAchieved,
                "At least 30% of assets in liquid tier",
                liquidPercent,
                30.0,
                "percent");
        thirtySeventyEntry.put("liquid_percent", liquidPercent);
        thirtySeventyEntry.put("growth_percent", growthPercent);
        goalsArray.add(thirtySeventyEntry);
        if (thirtySeventyAchieved) achievedCount++;

        // Goal 3 — Freedom Runway
        double currentRunwayMonths = monthlyBudgetCap > 0
                ? (liquidBalance + semiLiquidBalance) / monthlyBudgetCap
                : 0.0;
        boolean runwayAchieved = currentRunwayMonths >= freedomRunwayMonths;
        ObjectNode runwayEntry = buildGoalEntry(
                "FREEDOM_RUNWAY",
                "Freedom Runway",
                runwayAchieved,
                "Liquid reserves cover " + (int) freedomRunwayMonths + " months of expenses",
                currentRunwayMonths,
                freedomRunwayMonths,
                "months");
        runwayEntry.put("current_months", currentRunwayMonths);
        goalsArray.add(runwayEntry);
        if (runwayAchieved) achievedCount++;

        // Goal 4 — Insurance Free
        double annualIncome = monthlyBudgetCap * 12.0;
        double insuranceTarget = annualIncome * insuranceMultiple;
        boolean insuranceAchieved = totalInvestmentValue >= insuranceTarget;
        goalsArray.add(buildGoalEntry(
                "INSURANCE_FREE",
                "Insurance Free",
                insuranceAchieved,
                "Investment portfolio covers " + (int) insuranceMultiple + "x annual income",
                totalInvestmentValue,
                insuranceTarget,
                "currency"));
        if (insuranceAchieved) achievedCount++;

        // Goal 5 — Year One
        boolean yearOneAchieved = familyNetWorth >= yearOneTarget;
        goalsArray.add(buildGoalEntry(
                "YEAR_ONE",
                "Year One",
                yearOneAchieved,
                "Family net worth reaches annual target",
                familyNetWorth,
                yearOneTarget,
                "currency"));
        if (yearOneAchieved) achievedCount++;

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set(GOALS_FIELD, goalsArray);
        payload.put("achieved_count", achievedCount);
        payload.put("total_count", 5);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY, payload.toString());
    }

    private ObjectNode buildGoalEntry(String goalId, String goalName, boolean achieved,
                                      String description, double currentValue, double targetValue, String unit) {
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(GOAL_ID_FIELD, goalId);
        entry.put(GOAL_NAME_FIELD, goalName);
        entry.put(STATUS_FIELD, achieved ? STATUS_ACHIEVED : STATUS_IN_PROGRESS);
        entry.put(DESCRIPTION_FIELD, description);
        entry.put(CURRENT_VALUE_FIELD, currentValue);
        entry.put(TARGET_VALUE_FIELD, targetValue);
        entry.put(UNIT_FIELD, unit);
        return entry;
    }

    // ── WEALTH_VALIDATION_REPORT_FAMILY (Epic 8 Phase 4) ──────────────────────

    /**
     * Epic 8 Phase 4 — validation gate (Use Case 8.4). Runs four checks against
     * existing Phase 3 snapshot payloads. Each check currently produces PASS or
     * WARNING; CRITICAL is a reserved severity (see Q16) not yet emitted by any
     * check. Overall status = WARNING if any check is WARNING, PASS if all PASS.
     *
     * <p>Check 1 — Category Resolution: are all accounts categorised?
     * Check 2 — Missing Growth Rate: do investment accounts have a configured rate?
     * Check 3 — EMI Data Completeness: do loan accounts have amortization metadata?
     * Check 4 — Budget Cap Set: is monthly_budget_cap configured in policy settings?
     *
     * <p>Per Q16 resolution: CRITICAL does not block other snapshots — runStep isolation
     * handles that. If a future check introduces CRITICAL, the validation engine
     * would produce it here; what the dashboard shows for a CRITICAL key is a
     * frontend concern.
     */
    void computeValidation(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }

        JsonNode policySettings = resolveAdminPolicySettings(adminId);
        java.util.Map<String, JsonNode> snapshots = loadSnapshotsAsMap(profileId);

        ArrayNode checksArray = MAPPER.createArrayNode();
        int passCount = 0;
        int warningCount = 0;

        // Check 1 — Category Resolution
        JsonNode categoryPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_CATEGORY_VALIDATION, MAPPER.createObjectNode());
        int uncategorizedCount = categoryPayload.path("uncategorized_count").asInt(0);
        if (uncategorizedCount > 0) {
            checksArray.add(buildCheckEntry(
                    "CATEGORY_RESOLUTION",
                    STATUS_WARNING,
                    uncategorizedCount + " account(s) have no expense category set",
                    STATUS_WARNING));
            warningCount++;
        } else {
            checksArray.add(buildCheckEntry(
                    "CATEGORY_RESOLUTION",
                    STATUS_PASS,
                    "All accounts have an expense category set",
                    STATUS_PASS));
            passCount++;
        }

        // Check 2 — Missing Growth Rate
        JsonNode growthPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_GROWTH_PROJECTION_FAMILY, MAPPER.createObjectNode());
        boolean missingRate = hasMissingGrowthRate(growthPayload);
        if (missingRate) {
            checksArray.add(buildCheckEntry(
                    "MISSING_GROWTH_RATE",
                    STATUS_WARNING,
                    "One or more investment accounts are missing a configured growth rate",
                    STATUS_WARNING));
            warningCount++;
        } else {
            checksArray.add(buildCheckEntry(
                    "MISSING_GROWTH_RATE",
                    STATUS_PASS,
                    "All tracked investment accounts have a growth rate configured",
                    STATUS_PASS));
            passCount++;
        }

        // Check 3 — EMI Data Completeness
        JsonNode emiPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY, MAPPER.createObjectNode());
        int memberCount = emiPayload.path(MEMBER_COUNT_FIELD).asInt(0);
        double totalEmi = emiPayload.path(TOTAL_MONTHLY_EMI_FIELD).asDouble(0.0);
        boolean emiIncomplete = memberCount > 0 && totalEmi == 0.0;
        if (emiIncomplete) {
            checksArray.add(buildCheckEntry(
                    "EMI_DATA_COMPLETENESS",
                    STATUS_WARNING,
                    "Loan accounts found but no amortization metadata set — EMI figures may be zero",
                    STATUS_WARNING));
            warningCount++;
        } else {
            checksArray.add(buildCheckEntry(
                    "EMI_DATA_COMPLETENESS",
                    STATUS_PASS,
                    "EMI data completeness check passed",
                    STATUS_PASS));
            passCount++;
        }

        // Check 4 — Budget Cap Set
        double budgetCap = policySettings.path("monthly_budget_cap").asDouble(0.0);
        if (budgetCap <= 0.0) {
            checksArray.add(buildCheckEntry(
                    "BUDGET_CAP_SET",
                    STATUS_WARNING,
                    "monthly_budget_cap not configured in policy settings — goals engine used a default value",
                    STATUS_WARNING));
            warningCount++;
        } else {
            checksArray.add(buildCheckEntry(
                    "BUDGET_CAP_SET",
                    STATUS_PASS,
                    "Monthly budget cap is configured",
                    STATUS_PASS));
            passCount++;
        }

        String overallStatus = warningCount > 0 ? STATUS_WARNING : STATUS_PASS;

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set(CHECKS_FIELD, checksArray);
        payload.put(OVERALL_STATUS_FIELD, overallStatus);
        payload.put("pass_count", passCount);
        payload.put("warning_count", warningCount);
        // "critical_count" is a reserved contract field (per OpenQuestions.md Q16) — no current
        // check escalates to CRITICAL, so it is always 0 until a future check introduces one.
        payload.put("critical_count", 0);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_VALIDATION_REPORT_FAMILY, payload.toString());
    }

    private ObjectNode buildCheckEntry(String checkId, String status, String message, String severity) {
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(CHECK_ID_FIELD, checkId);
        entry.put(STATUS_FIELD, status);
        entry.put(MESSAGE_FIELD, message);
        entry.put(SEVERITY_FIELD, severity);
        return entry;
    }

    /**
     * Returns true if any projection entry in WEALTH_GROWTH_PROJECTION_FAMILY has
     * a missing growth rate — represented by the absence of a projection entry
     * (account_type not in tracked set means it was excluded, which is correct; the
     * concern is investment-type accounts that somehow ended up in the projections
     * array but with rate == 0). For Phase 4, we flag any projection entry where
     * the growth_rate_annual value is 0 or missing, since all tracked types have
     * strictly positive rates (MUTUAL_FUND=12%, NPS=10%, PPF=7.1%).
     */
    private boolean hasMissingGrowthRate(JsonNode growthPayload) {
        JsonNode projections = growthPayload.path("projections");
        if (!projections.isArray()) {
            return false;
        }
        for (JsonNode proj : projections) {
            if (proj.path("growth_rate_annual").asDouble(0.0) <= 0.0) {
                return true;
            }
        }
        return false;
    }

    // ── ACTION_CENTER_ALERTS_FAMILY (v0.5 Phase 3, OpenQuestions.md Q30) ──────

    /**
     * Consolidated Action Center — a single read-only alert feed across all three
     * domains, aggregated per household member (per Q30: upcoming events and
     * biometric streak gaps are evaluated per-profile, matching ADR-017's rule
     * that vitals/events stay per-person and are never summed; only the vehicle
     * compliance list and the overall payload are gathered into one family view).
     *
     * <p>Three alert types (Q30 resolution):
     * <ul>
     *   <li>Upcoming calendar events — same 30-day lookahead window as
     *       {@link #computeEventSummary(UUID)}, evaluated per member.</li>
     *   <li>Vehicle compliance deadlines — PUC/insurance expiry within the next
     *       30 days or already expired, reusing {@link ExpiryDateUtil} (the same
     *       helper the Vacation Planner uses for the identical JSONB parsing).</li>
     *   <li>Biometric streak gaps — WEIGHT, BLOOD_PRESSURE, BLOOD_SUGAR_FASTING
     *       (Q30's "core 3") with no reading in the last 30 days. A vital type
     *       with zero readings ever is also flagged (treated as an infinite gap)
     *       rather than silently skipped — matches the "honest gap reporting"
     *       precedent set by computeCategoryValidation in Phase 1.</li>
     * </ul>
     */
    void computeActionCenterAlerts(UUID profileId, LocalDate today) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }
        JsonNode membersArray = profileServiceClient.listProfiles(adminId, true).path(PROFILES_FIELD);

        String todayText = today.toString();
        String lookaheadText = today.plusDays(ACTION_CENTER_EVENT_LOOKAHEAD_DAYS).toString();

        ArrayNode upcomingEvents = MAPPER.createArrayNode();
        ArrayNode vehicleCompliance = MAPPER.createArrayNode();
        ArrayNode streakGaps = MAPPER.createArrayNode();
        int memberCount = 0;

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            UUID memberProfileId = UUID.fromString(memberProfileIdText);
            String memberName = member.path(FULL_NAME_FIELD).asText("");

            addUpcomingEventsForMember(memberProfileId, memberName, todayText, lookaheadText, upcomingEvents);
            addVehicleComplianceForMember(memberProfileId, memberName, today, vehicleCompliance);
            addStreakGapsForMember(memberProfileId, memberName, today, streakGaps);
            memberCount++;
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set(UPCOMING_EVENTS_FIELD, upcomingEvents);
        payload.set(VEHICLE_COMPLIANCE_FIELD, vehicleCompliance);
        payload.set(BIOMETRIC_STREAK_GAPS_FIELD, streakGaps);
        payload.put(MEMBER_COUNT_FIELD, memberCount);
        snapshotRepository.upsert(profileId, SnapshotKey.ACTION_CENTER_ALERTS_FAMILY, payload.toString());
    }

    private void addUpcomingEventsForMember(
            UUID memberProfileId, String memberName, String today, String lookaheadText, ArrayNode upcomingEvents) {
        JsonNode eventsResponse = householdServiceClient.listCalendarEvents(
                memberProfileId, null, today, lookaheadText, null, null);
        JsonNode eventsArray = eventsResponse.path(EVENTS_FIELD);
        if (!eventsArray.isArray()) {
            return;
        }
        for (JsonNode event : eventsArray) {
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put(PROFILE_ID_FIELD, memberProfileId.toString());
            entry.put(FULL_NAME_FIELD, memberName);
            entry.put("id", event.path("id").asText(""));
            entry.put(TITLE_FIELD, event.path(TITLE_FIELD).asText(""));
            entry.put(START_DATE_FIELD, event.path(START_DATE_FIELD).asText(""));
            upcomingEvents.add(entry);
        }
    }

    private void addVehicleComplianceForMember(
            UUID memberProfileId, String memberName, LocalDate today, ArrayNode vehicleCompliance) {
        JsonNode assetsResponse = wealthServiceClient.listPhysicalAssets(
                VEHICLE_ASSET_TYPE, true, memberProfileId.toString(), null, null);
        JsonNode assetsArray = assetsResponse.path(PHYSICAL_ASSETS_FIELD);
        if (!assetsArray.isArray()) {
            return;
        }
        LocalDate warningThreshold = today.plusDays(ACTION_CENTER_EVENT_LOOKAHEAD_DAYS);
        for (JsonNode asset : assetsArray) {
            addComplianceIssueIfDueBy(asset, memberProfileId, memberName, "puc_expiry", "PUC", warningThreshold, vehicleCompliance);
            addComplianceIssueIfDueBy(asset, memberProfileId, memberName, "insurance_expiry", "INSURANCE", warningThreshold, vehicleCompliance);
        }
    }

    private void addComplianceIssueIfDueBy(
            JsonNode asset, UUID memberProfileId, String memberName, String metadataKey, String issueLabel,
            LocalDate warningThreshold, ArrayNode vehicleCompliance) {
        LocalDate expiryDate = ExpiryDateUtil.parse(asset.path(METADATA_FIELD), metadataKey);
        if (expiryDate == null || expiryDate.isAfter(warningThreshold)) {
            return;
        }
        ObjectNode issue = MAPPER.createObjectNode();
        issue.put(PROFILE_ID_FIELD, memberProfileId.toString());
        issue.put(FULL_NAME_FIELD, memberName);
        issue.put(ASSET_ID_FIELD, asset.path(ASSET_ID_FIELD).asText(""));
        issue.put(ASSET_NAME_FIELD, asset.path(ASSET_NAME_FIELD).asText(""));
        issue.put("issue_type", issueLabel + "_EXPIRED");
        issue.put("expiry_date", asset.path(METADATA_FIELD).path(metadataKey).asText(""));
        vehicleCompliance.add(issue);
    }

    private void addStreakGapsForMember(UUID memberProfileId, String memberName, LocalDate today, ArrayNode streakGaps) {
        JsonNode vitalsResponse = healthServiceClient.listVitals(memberProfileId, null, 0, VITALS_AGGREGATION_FETCH_SIZE);
        JsonNode vitalsArray = vitalsResponse.path(VITAL_READINGS_FIELD);

        java.util.Map<String, String> latestReadingDateByType = new java.util.HashMap<>();
        if (vitalsArray.isArray()) {
            for (JsonNode vital : vitalsArray) {
                String vitalType = vital.path(VITAL_TYPE_KEY).asText("");
                String readingDate = vital.path(READING_DATE_FIELD).asText("");
                // Same newest-first assumption as computeVitalsSummary — only the first date seen per type is kept
                latestReadingDateByType.putIfAbsent(vitalType, readingDate);
            }
        }

        for (String vitalType : STREAK_TRACKED_VITAL_TYPES) {
            String lastReadingDateText = latestReadingDateByType.get(vitalType);
            addStreakGapIfOverdue(memberProfileId, memberName, vitalType, lastReadingDateText, today, streakGaps);
        }
    }

    private void addStreakGapIfOverdue(
            UUID memberProfileId, String memberName, String vitalType, String lastReadingDateText,
            LocalDate today, ArrayNode streakGaps) {
        LocalDate lastReadingDate = null;
        if (lastReadingDateText != null && !lastReadingDateText.isBlank()) {
            try {
                lastReadingDate = LocalDate.parse(lastReadingDateText);
            } catch (java.time.format.DateTimeParseException e) {
                AppLogger.info("ActionCenter: unparseable reading_date '%s' for profile %s, treating as no reading",
                        lastReadingDateText, memberProfileId);
            }
        }

        long daysSinceLastReading = lastReadingDate == null
                ? Long.MAX_VALUE
                : java.time.temporal.ChronoUnit.DAYS.between(lastReadingDate, today);
        if (daysSinceLastReading < STREAK_GAP_THRESHOLD_DAYS) {
            return;
        }

        // NOTE: `lastReadingDate == null ? null : (int) x` (not `(Integer) null`) is
        // required here — mixing a boxed-cast null with a primitive int branch makes
        // the ternary apply binary numeric promotion and unbox the null, throwing NPE.
        Integer daysSinceLastReadingBoxed = lastReadingDate == null ? null : (int) daysSinceLastReading;

        ObjectNode gap = MAPPER.createObjectNode();
        gap.put(PROFILE_ID_FIELD, memberProfileId.toString());
        gap.put(FULL_NAME_FIELD, memberName);
        gap.put(VITAL_TYPE_KEY, vitalType);
        gap.put(LAST_READING_DATE_FIELD, lastReadingDate == null ? null : lastReadingDateText);
        gap.put("days_since_last_reading", daysSinceLastReadingBoxed);
        streakGaps.add(gap);
    }

    // ── snapshot read helpers (Phase 4) ──────────────────────────────────────

    /**
     * Loads all stored snapshots for the given profile into a key→parsed-JsonNode map.
     * Used by the Phase 4 engines to read Phase 3 outputs without re-calling domain REST services.
     */
    private java.util.Map<String, JsonNode> loadSnapshotsAsMap(UUID profileId) {
        java.util.Map<String, JsonNode> result = new java.util.HashMap<>();
        for (DashboardSnapshotDto dto : snapshotRepository.findByProfileId(profileId)) {
            try {
                result.put(dto.getSnapshotKey(), MAPPER.readTree(dto.getPayload()));
            } catch (Exception e) {
                AppLogger.error("ProjectionEngine: failed to parse snapshot %s for profile %s", e,
                        dto.getSnapshotKey(), profileId);
            }
        }
        return result;
    }

    /**
     * Reads admin.policy_settings JSONB from the profile service.
     * Returns an empty node if the field is absent or if the call fails — all policy
     * values have safe defaults applied in the goals/validation engines.
     */
    private JsonNode resolveAdminPolicySettings(UUID adminId) {
        try {
            JsonNode admin = profileServiceClient.getAdmin(adminId);
            JsonNode policySettings = admin.path(POLICY_SETTINGS_FIELD);
            return policySettings.isMissingNode() ? MAPPER.createObjectNode() : policySettings;
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: could not load admin policy_settings for admin %s, using defaults", adminId);
            return MAPPER.createObjectNode();
        }
    }

    // ── household resolution helper ───────────────────────────────────────────

    /**
     * Resolves the admin_id for the given profile by calling the profile service.
     * Returns null (and logs a warning) if the profile has no admin_id — in that case
     * the caller should skip household rollup silently.
     */
    private UUID resolveAdminId(UUID profileId) {
        JsonNode ownProfile = profileServiceClient.getProfile(profileId);
        String adminIdText = ownProfile.path("admin_id").asText("");
        if (adminIdText.isEmpty()) {
            AppLogger.info("ProjectionEngine: profile %s has no admin_id, skipping household rollup", profileId);
            return null;
        }
        return UUID.fromString(adminIdText);
    }
}
