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

    private static final double DEFAULT_MONTHLY_BUDGET_CAP = 0.0;
    // ADR-022: default changes 6 → 360; debt_crossover_threshold_percent,
    // insurance_multiple and year_one_annual_target go dead (no longer read) —
    // the corrected formulas use fixed 100/30/100 thresholds instead.
    private static final double DEFAULT_FREEDOM_RUNWAY_MONTHS = 360.0;

    // ADR-022 Phase 1 — corrected formula goals engine field/constant names.
    private static final String RELATION_TO_ADMIN_FIELD = "relation_to_admin";
    private static final String RELATION_SELF = "SELF";
    private static final String RELATION_SPOUSE = "SPOUSE";
    private static final String RELATION_CHILD = "CHILD";
    private static final String FD_ACCOUNT_TYPE = "FD";
    private static final String MAXGAIN_PURPOSE_TAG = "MaxGain";
    private static final String PURPOSE_TAG_FIELD = "purpose_tag";
    private static final String LIQUIDITY_TIER_FIELD = "liquidity_tier";
    private static final String TRANSACTIONS_FIELD = "transactions";
    private static final String AMOUNT_FIELD = "amount";
    private static final String CATEGORY_FIELD = "category";
    private static final String TOTAL_OUTSTANDING_BALANCE_FIELD = "total_outstanding_balance";
    private static final String GOAL_TYPE_FIELD = "goal_type";
    private static final String GOAL_PLANS_FIELD = "goal_plans";
    private static final String BENEFICIARY_PROFILE_ID_FIELD = "beneficiary_profile_id";
    private static final String BENEFICIARY_NAME_FIELD = "beneficiary_name";
    private static final String YEAR_ONE_GOAL_TYPE = "YEAR_ONE";
    private static final String THIRTY_SEVENTY_GOAL_TYPE = "THIRTY_SEVENTY_TARGET";
    // ADR-022 Phase 2 — insurance_policy field name constants for the
    // THIRTY_SEVENTY_TARGET premium term.
    private static final String INSURANCE_POLICIES_FIELD = "insurance_policies";
    private static final String PREMIUM_AMOUNT_FIELD = "premium_amount";
    private static final String PREMIUM_FREQUENCY_FIELD = "premium_frequency";
    private static final String IS_ACTIVE_FIELD = "is_active";
    private static final String FREQUENCY_ANNUAL = "ANNUAL";
    private static final int MONTHS_PER_YEAR = 12;
    private static final double DEBT_CROSSOVER_TARGET_PERCENT = 100.0;
    private static final double THIRTY_SEVENTY_TARGET_PERCENT = 30.0;
    private static final double INSURANCE_FREE_TARGET_PERCENT = 100.0;
    private static final double YEAR_ONE_TARGET_PERCENT = 100.0;
    private static final double YEAR_ONE_FUNDING_RATIO = 0.25;
    private static final int THIRTY_SEVENTY_LOOKBACK_MONTHS = 3;
    private static final int TRANSACTION_AGGREGATION_FETCH_SIZE = 200;
    private static final java.util.Set<String> NON_DISCRETIONARY_CATEGORIES =
            java.util.Set.of("HOUSEHOLD_CORE", "CHILD_RELATED", "MAINTENANCE");
    private static final java.util.Set<String> INCOME_CATEGORIES =
            java.util.Set.of("SALARY", "RENTAL", "OTHER_INCOME");

    // ADR-022 Phase 3 — computeGoalDetail() field name constants.
    private static final String GOAL_DETAILS_FIELD = "goal_details";
    private static final String MILESTONES_FIELD = "milestones";
    private static final String RULES_FIELD = "rules";
    private static final String TRIGGER_EVENTS_FIELD = "trigger_events";
    private static final String OBJECTIVE_FIELD = "objective";
    private static final String TARGET_STATE_FIELD = "target_state";
    private static final String DETAIL_FIELD = "detail";
    private static final String ID_FIELD = "id";
    private static final String SEQUENCE_NO_FIELD = "sequence_no";
    private static final String LABEL_FIELD = "label";
    private static final String SIGNIFICANCE_FIELD = "significance";
    private static final String IS_MANUAL_CHECKLIST_FIELD = "is_manual_checklist";
    private static final String IS_ACHIEVED_FIELD = "is_achieved";
    private static final String POLICY_NAME_FIELD = "policy_name";
    private static final String PROVIDER_FIELD = "provider";
    private static final String POLICY_TYPE_FIELD = "policy_type";
    private static final String COVERAGE_AMOUNT_FIELD = "coverage_amount";
    private static final String PAYOUT_STRUCTURE_FIELD = "payout_structure";
    private static final String INSURANCE_FREE_GOAL_TYPE = "INSURANCE_FREE";

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
     * 11 computeGoalDetail            — WEALTH_GOAL_DETAIL_FAMILY (ADR-022 Phase 3;
     *                                    must run after computeFormulaGoals — reads its
     *                                    just-written snapshot from the same pass)
     * 12 computeValidation            — WEALTH_VALIDATION_REPORT_FAMILY
     * 13 computeActionCenterAlerts    — ACTION_CENTER_ALERTS_FAMILY
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
        runStep("computeGoalDetail", profileId, this::computeGoalDetail);
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

    // ── WEALTH_FORMULA_GOALS_FAMILY (Epic 8 Phase 4, corrected ADR-022 Phase 1) ─

    /**
     * ADR-022 Phase 1 — corrected formula goals engine. This is a bugfix to the
     * Epic 8 Phase 4 formulas in place, not a new parallel system: same 5
     * goal_ids, same snapshot key, same Dashboard consumer. See ADR-022's
     * old-vs-new formula table for the full derivation of each corrected formula.
     *
     * <p>Goal 1 — Debt Crossover: family MF corpus (SELF+SPOUSE only) / outstanding
     *   loan balance (HOME_LOAN/PERSONAL_LOAN/CAR_LOAN, excl. CHILD-relation loans)
     *   * 100, achieved &gt;= 100 (direction flipped from the old shipped formula).
     * Goal 2 — 30-70 Target: (EMI + non-discretionary DEBIT spend avg + insurance
     *   premiums [ADR-022 Phase 2: real sum across active wealth.insurance_policy
     *   rows, normalized to monthly — ANNUAL / 12, MONTHLY pass-through]) / avg
     *   income-tagged CREDIT * 100, achieved &lt;= 30 (the sole "lower is better"
     *   exception).
     * Goal 3 — Freedom Runway: own core-runway-capital aggregation (excludes PPF
     *   and CHILD-relation accounts, not a reuse of WEALTH_LIQUIDITY_TIERS_FAMILY)
     *   / monthlyBudgetCap, achieved &gt;= freedom_runway_months (default 360).
     * Goal 4 — Insurance Free: (MaxGain-tagged + FD balances) / (outstanding debt +
     *   legal fees + academic buffer) * 100, achieved &gt;= 100.
     * Goal 5 — Year One: per CHILD-relation profile with a configured YEAR_ONE
     *   goal_plan row, (that child's MF balance) / (25% of future education cost)
     *   * 100, achieved &gt;= 100. total_count is therefore no longer fixed at 5 —
     *   it is 4 + the number of children with a configured YEAR_ONE goal_plan.
     */
    void computeFormulaGoals(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }

        JsonNode policySettings = resolveAdminPolicySettings(adminId);
        java.util.Map<String, JsonNode> snapshots = loadSnapshotsAsMap(profileId);
        JsonNode emiPayload = snapshots.getOrDefault(SnapshotKey.WEALTH_EMI_TRACKING_FAMILY, MAPPER.createObjectNode());
        JsonNode membersArray = profileServiceClient.listProfiles(adminId, true).path(PROFILES_FIELD);

        ArrayNode goalsArray = MAPPER.createArrayNode();
        goalsArray.add(buildDebtCrossoverGoal(membersArray));
        goalsArray.add(buildThirtySeventyGoal(membersArray, emiPayload, adminId));
        goalsArray.add(buildFreedomRunwayGoal(membersArray, policySettings));
        goalsArray.add(buildInsuranceFreeGoal(membersArray, emiPayload, policySettings));
        buildYearOneGoals(adminId, membersArray).forEach(goalsArray::add);

        int achievedCount = 0;
        for (JsonNode goal : goalsArray) {
            if (STATUS_ACHIEVED.equals(goal.path(STATUS_FIELD).asText(""))) {
                achievedCount++;
            }
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set(GOALS_FIELD, goalsArray);
        payload.put("achieved_count", achievedCount);
        payload.put("total_count", goalsArray.size());
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY, payload.toString());
    }

    private ObjectNode buildDebtCrossoverGoal(JsonNode membersArray) {
        double mfCorpusSelfSpouse = 0.0;
        double outstandingLoansNonChild = 0.0;

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            String relation = member.path(RELATION_TO_ADMIN_FIELD).asText("");
            UUID memberProfileId = UUID.fromString(memberProfileIdText);

            if (RELATION_SELF.equals(relation) || RELATION_SPOUSE.equals(relation)) {
                mfCorpusSelfSpouse += sumAccountTypeBalanceForMember(memberProfileIdText, memberProfileId, INV_TYPE_MUTUAL_FUND);
            }
            if (!RELATION_CHILD.equals(relation)) {
                double[] loanTotals = new double[3];
                buildMemberLoanEntries(memberProfileIdText, loanTotals);
                outstandingLoansNonChild += loanTotals[0];
            }
        }

        double debtPercent = outstandingLoansNonChild > 0 ? mfCorpusSelfSpouse / outstandingLoansNonChild * 100.0 : 0.0;
        return buildGoalEntry(
                "DEBT_CROSSOVER", "Debt Crossover",
                "Family mutual fund corpus (self & spouse) covers outstanding loan balance",
                debtPercent, DEBT_CROSSOVER_TARGET_PERCENT, "percent");
    }

    private ObjectNode buildThirtySeventyGoal(JsonNode membersArray, JsonNode emiPayload, UUID adminId) {
        double monthlyEmi = emiPayload.path(TOTAL_MONTHLY_EMI_FIELD).asDouble(0.0);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate lookbackStart = today.minusMonths(THIRTY_SEVENTY_LOOKBACK_MONTHS);

        double nonDiscretionaryDebitTotal = 0.0;
        double incomeCreditTotal = 0.0;
        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            double[] memberTotals = sumThirtySeventyTxnsForMember(memberProfileIdText, lookbackStart, today);
            nonDiscretionaryDebitTotal += memberTotals[0];
            incomeCreditTotal += memberTotals[1];
        }

        double nonDiscretionaryMonthlyAvg = nonDiscretionaryDebitTotal / THIRTY_SEVENTY_LOOKBACK_MONTHS;
        double incomeMonthlyAvg = incomeCreditTotal / THIRTY_SEVENTY_LOOKBACK_MONTHS;
        double insurancePremiums = sumMonthlyInsurancePremiums(adminId);
        double numerator = monthlyEmi + nonDiscretionaryMonthlyAvg + insurancePremiums;
        double thirtySeventyPercent = incomeMonthlyAvg > 0 ? numerator / incomeMonthlyAvg * 100.0 : 0.0;

        return buildGoalEntry(
                THIRTY_SEVENTY_GOAL_TYPE, "30-70 Target",
                "Essential outflows (EMI + non-discretionary spend + insurance) stay under 30% of average income",
                thirtySeventyPercent, THIRTY_SEVENTY_TARGET_PERCENT, "percent");
    }

    /**
     * ADR-022 Phase 2 — sums {@code premium_amount} across every active
     * {@code wealth.insurance_policy} row for this household, normalized to a
     * monthly figure: ANNUAL policies divide by 12, MONTHLY policies pass
     * through unchanged. Feeds {@link #buildThirtySeventyGoal}'s numerator.
     * Mirrors {@link #buildYearOneGoals}'s tolerant error handling — a downstream
     * failure here must not break the rest of computeFormulaGoals.
     */
    private double sumMonthlyInsurancePremiums(UUID adminId) {
        JsonNode policies;
        try {
            policies = wealthServiceClient.listInsurancePolicies(adminId).path(INSURANCE_POLICIES_FIELD);
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: could not load insurance policies for admin %s, treating premiums as 0", adminId);
            return 0.0;
        }
        if (!policies.isArray()) {
            return 0.0;
        }

        double totalMonthlyPremium = 0.0;
        for (JsonNode policy : policies) {
            if (!policy.path(IS_ACTIVE_FIELD).asBoolean(false)) {
                continue;
            }
            double premiumAmount = policy.path(PREMIUM_AMOUNT_FIELD).asDouble(0.0);
            boolean isAnnual = FREQUENCY_ANNUAL.equals(policy.path(PREMIUM_FREQUENCY_FIELD).asText(""));
            totalMonthlyPremium += isAnnual ? premiumAmount / MONTHS_PER_YEAR : premiumAmount;
        }
        return totalMonthlyPremium;
    }

    /**
     * Per-member trailing-window transaction aggregation for {@link #buildThirtySeventyGoal}.
     * First _FAMILY step to page through full transaction history rather than
     * account-level balances (ADR-022 flags this as a perf watch-item, not blocking).
     */
    private double[] sumThirtySeventyTxnsForMember(String memberProfileIdText, LocalDate from, LocalDate to) {
        double[] totals = new double[2]; // [nonDiscretionaryDebit, incomeCredit]
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            String accountIdText = account.path(ACCOUNT_ID_FIELD).asText("");
            if (accountIdText.isEmpty()) {
                continue;
            }
            UUID accountId = UUID.fromString(accountIdText);
            totals[0] += sumTransactionsByCategory(accountId, memberProfileIdText, from, to, "DEBIT", NON_DISCRETIONARY_CATEGORIES);
            totals[1] += sumTransactionsByCategory(accountId, memberProfileIdText, from, to, "CREDIT", INCOME_CATEGORIES);
        }
        return totals;
    }

    private double sumTransactionsByCategory(UUID accountId, String memberProfileIdText, LocalDate from, LocalDate to,
                                              String txnType, java.util.Set<String> categories) {
        JsonNode response = wealthServiceClient.listTransactions(
                accountId, memberProfileIdText, from.toString(), to.toString(), txnType, 0, TRANSACTION_AGGREGATION_FETCH_SIZE);
        double total = 0.0;
        JsonNode transactions = response.path(TRANSACTIONS_FIELD);
        if (transactions.isArray()) {
            for (JsonNode txn : transactions) {
                String category = txn.path(METADATA_FIELD).path(CATEGORY_FIELD).asText("");
                if (categories.contains(category)) {
                    total += txn.path(AMOUNT_FIELD).asDouble(0.0);
                }
            }
        }
        return total;
    }

    private ObjectNode buildFreedomRunwayGoal(JsonNode membersArray, JsonNode policySettings) {
        double monthlyBudgetCap = policySettings.path("monthly_budget_cap").asDouble(DEFAULT_MONTHLY_BUDGET_CAP);
        double freedomRunwayMonths = policySettings.path("freedom_runway_months").asDouble(DEFAULT_FREEDOM_RUNWAY_MONTHS);

        double coreRunwayCapital = 0.0;
        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            if (RELATION_CHILD.equals(member.path(RELATION_TO_ADMIN_FIELD).asText(""))) {
                continue; // children's accounts excluded (ADR-022 audit finding)
            }
            UUID memberProfileId = UUID.fromString(memberProfileIdText);
            coreRunwayCapital += sumCoreRunwayCapitalForMember(memberProfileIdText, memberProfileId);
        }

        double currentRunwayMonths = monthlyBudgetCap > 0 ? coreRunwayCapital / monthlyBudgetCap : 0.0;
        ObjectNode entry = buildGoalEntry(
                "FREEDOM_RUNWAY", "Freedom Runway",
                "Core runway capital (excl. PPF and children's accounts) covers "
                        + (int) freedomRunwayMonths + " months of expenses",
                currentRunwayMonths, freedomRunwayMonths, "months");
        entry.put("current_months", currentRunwayMonths);
        return entry;
    }

    /**
     * New aggregation, not a reuse of {@link #accumulateTiersForMember} — the
     * shared liquidity-tier totals have no PPF/CHILD exclusion, which ADR-022's
     * audit found are both required for FREEDOM_RUNWAY specifically.
     */
    private double sumCoreRunwayCapitalForMember(String memberProfileIdText, UUID memberProfileId) {
        double total = 0.0;
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            if (INV_TYPE_PPF.equals(account.path(ACCOUNT_TYPE_FIELD).asText(""))) {
                continue; // PPF excluded from core runway capital (ADR-022 audit finding)
            }
            String tier = account.path(METADATA_FIELD).path(LIQUIDITY_TIER_FIELD).asText("").trim();
            if (TIER_LIQUID.equals(tier) || TIER_SEMI_LIQUID.equals(tier)) {
                total += currentBalanceFor(account, memberProfileId);
            }
        }
        return total;
    }

    private ObjectNode buildInsuranceFreeGoal(JsonNode membersArray, JsonNode emiPayload, JsonNode policySettings) {
        double maxGainAndFdBalance = 0.0;
        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty()) {
                continue;
            }
            UUID memberProfileId = UUID.fromString(memberProfileIdText);
            maxGainAndFdBalance += sumMaxGainOrFdBalanceForMember(memberProfileIdText, memberProfileId);
        }

        double outstandingDebt = emiPayload.path(TOTAL_OUTSTANDING_BALANCE_FIELD).asDouble(0.0);
        double legalFees = policySettings.path("insurance_free_legal_fees").asDouble(0.0);
        double academicBuffer = policySettings.path("insurance_free_academic_buffer").asDouble(0.0);
        double denominator = outstandingDebt + legalFees + academicBuffer;
        double insurancePercent = denominator > 0 ? maxGainAndFdBalance / denominator * 100.0 : 0.0;

        return buildGoalEntry(
                "INSURANCE_FREE", "Insurance Free",
                "MaxGain and FD balances cover outstanding debt plus legal fees and academic buffer",
                insurancePercent, INSURANCE_FREE_TARGET_PERCENT, "percent");
    }

    private double sumMaxGainOrFdBalanceForMember(String memberProfileIdText, UUID memberProfileId) {
        double total = 0.0;
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            boolean isFd = FD_ACCOUNT_TYPE.equals(account.path(ACCOUNT_TYPE_FIELD).asText(""));
            boolean isMaxGain = MAXGAIN_PURPOSE_TAG.equals(account.path(METADATA_FIELD).path(PURPOSE_TAG_FIELD).asText(""));
            if (isFd || isMaxGain) {
                total += currentBalanceFor(account, memberProfileId);
            }
        }
        return total;
    }

    /**
     * ADR-022's one deliberate exception to "computeFormulaGoals and goal_plan are
     * fully decoupled" — YEAR_ONE's education inputs have no other home. A CHILD
     * profile with no configured YEAR_ONE goal_plan row is omitted entirely (not
     * just from this richer view but from the base card too); a configured child
     * with zero MUTUAL_FUND accounts still gets an entry with current_value=0.
     */
    private List<ObjectNode> buildYearOneGoals(UUID adminId, JsonNode membersArray) {
        List<ObjectNode> entries = new java.util.ArrayList<>();
        JsonNode goalPlans;
        try {
            goalPlans = wealthServiceClient.listGoalPlans(adminId).path(GOAL_PLANS_FIELD);
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: could not load goal plans for admin %s, skipping YEAR_ONE", adminId);
            return entries;
        }
        if (!goalPlans.isArray()) {
            return entries;
        }

        for (JsonNode member : membersArray) {
            String memberProfileIdText = member.path(PROFILE_ID_FIELD).asText("");
            if (memberProfileIdText.isEmpty() || !RELATION_CHILD.equals(member.path(RELATION_TO_ADMIN_FIELD).asText(""))) {
                continue;
            }
            JsonNode yearOnePlan = findYearOnePlan(goalPlans, memberProfileIdText);
            if (yearOnePlan == null) {
                continue;
            }
            entries.add(buildYearOneGoalEntry(memberProfileIdText, member.path(FULL_NAME_FIELD).asText(""), yearOnePlan));
        }
        return entries;
    }

    private JsonNode findYearOnePlan(JsonNode goalPlans, String beneficiaryProfileIdText) {
        for (JsonNode plan : goalPlans) {
            if (YEAR_ONE_GOAL_TYPE.equals(plan.path(GOAL_TYPE_FIELD).asText(""))
                    && beneficiaryProfileIdText.equals(plan.path(BENEFICIARY_PROFILE_ID_FIELD).asText(""))) {
                return plan;
            }
        }
        return null;
    }

    private ObjectNode buildYearOneGoalEntry(String childProfileIdText, String childName, JsonNode yearOnePlan) {
        UUID childProfileId = UUID.fromString(childProfileIdText);
        double baseCost = yearOnePlan.path("education_base_cost").asDouble(0.0);
        double inflationRate = yearOnePlan.path("education_inflation_rate").asDouble(0.0);
        int yearsToEntry = yearOnePlan.path("education_years_to_entry").asInt(0);
        double futureCost = baseCost * Math.pow(1.0 + inflationRate, yearsToEntry);
        double fundingTarget = YEAR_ONE_FUNDING_RATIO * futureCost;

        double mfBalance = sumAccountTypeBalanceForMember(childProfileIdText, childProfileId, INV_TYPE_MUTUAL_FUND);
        double fundedPercent = fundingTarget > 0 ? mfBalance / fundingTarget * 100.0 : 0.0;

        ObjectNode entry = buildGoalEntry(
                YEAR_ONE_GOAL_TYPE, "Year One — " + childName,
                "Mutual fund corpus covers 25% of " + childName + "'s projected education cost at entry",
                fundedPercent, YEAR_ONE_TARGET_PERCENT, "percent");
        entry.put(BENEFICIARY_PROFILE_ID_FIELD, childProfileIdText);
        entry.put(BENEFICIARY_NAME_FIELD, childName);
        return entry;
    }

    /**
     * Fetches all of a member's accounts unfiltered and matches account_type in
     * Java — same "fetch-all, filter client-side" convention every other step in
     * this engine already uses (buildLoanEntry, accumulateTiersForMember,
     * buildProjectionEntry), rather than introducing a new accountType-filtered
     * call shape.
     */
    private double sumAccountTypeBalanceForMember(String memberProfileIdText, UUID memberProfileId, String accountType) {
        double total = 0.0;
        JsonNode accounts = wealthServiceClient.listAccounts(null, true, memberProfileIdText);
        for (JsonNode account : accounts.path(ACCOUNTS_FIELD)) {
            if (accountType.equals(account.path(ACCOUNT_TYPE_FIELD).asText(""))) {
                total += currentBalanceFor(account, memberProfileId);
            }
        }
        return total;
    }

    private ObjectNode buildGoalEntry(String goalId, String goalName, String description,
                                       double currentValue, double targetValue, String unit) {
        boolean achieved = isGoalAchieved(goalId, currentValue, targetValue);
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

    /**
     * Explicit per-goal-type achieved-direction lookup (ADR-022) — THIRTY_SEVENTY_TARGET
     * is the sole "lower is better" exception; every other goal type is "higher is
     * better". Deliberately not a single hardcoded "except goal X" branch — that shape
     * is exactly what silently broke once DEBT_CROSSOVER's direction flipped and
     * THIRTY_SEVENTY_TARGET became the new exception. Package-private for direct
     * unit testing of the lookup itself.
     */
    boolean isGoalAchieved(String goalId, double currentValue, double targetValue) {
        if (THIRTY_SEVENTY_GOAL_TYPE.equals(goalId)) {
            return currentValue <= targetValue;
        }
        return currentValue >= targetValue;
    }

    // ── WEALTH_GOAL_DETAIL_FAMILY (ADR-022 Phase 3) ────────────────────────────

    /**
     * Merges each configured {@code wealth.goal_plan} row with its live
     * WEALTH_FORMULA_GOALS_FAMILY entry (already written earlier in this same
     * refreshAll pass — read back via {@link #loadSnapshotsAsMap}, the same pattern
     * {@link #computeValidation} uses for WEALTH_EMI_TRACKING_FAMILY). A goal_plan row
     * with no live match is skipped, not errored (defensive — shouldn't normally
     * happen). A goal type with no configured goal_plan row is simply absent from this
     * snapshot; WEALTH_FORMULA_GOALS_FAMILY still shows every live goal regardless.
     */
    void computeGoalDetail(UUID profileId) {
        UUID adminId = resolveAdminId(profileId);
        if (adminId == null) {
            return;
        }

        JsonNode formulaGoals = loadSnapshotsAsMap(profileId)
                .getOrDefault(SnapshotKey.WEALTH_FORMULA_GOALS_FAMILY, MAPPER.createObjectNode())
                .path(GOALS_FIELD);

        JsonNode goalPlans;
        try {
            goalPlans = wealthServiceClient.listGoalPlans(adminId).path(GOAL_PLANS_FIELD);
        } catch (RuntimeException e) {
            AppLogger.info("ProjectionEngine: could not load goal plans for admin %s, skipping goal detail", adminId);
            return;
        }
        if (!goalPlans.isArray()) {
            return;
        }

        ArrayNode goalDetailsArray = MAPPER.createArrayNode();
        for (JsonNode plan : goalPlans) {
            JsonNode matchedGoal = findMatchingFormulaGoal(formulaGoals, plan);
            if (matchedGoal == null) {
                continue;
            }
            ObjectNode entry = buildGoalDetailEntry(plan, matchedGoal);
            if (INSURANCE_FREE_GOAL_TYPE.equals(plan.path(GOAL_TYPE_FIELD).asText(""))) {
                entry.set(INSURANCE_POLICIES_FIELD, activeInsurancePolicies(adminId));
            }
            goalDetailsArray.add(entry);
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.set(GOAL_DETAILS_FIELD, goalDetailsArray);
        snapshotRepository.upsert(profileId, SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY, payload.toString());
    }

    /**
     * Join key: goal_id == goal_type for the 4 singleton types; goal_id == goal_type
     * AND beneficiary_profile_id for YEAR_ONE (YEAR_ONE formula-goal entries already
     * carry beneficiary_profile_id — see buildYearOneGoalEntry).
     */
    private JsonNode findMatchingFormulaGoal(JsonNode formulaGoals, JsonNode plan) {
        if (!formulaGoals.isArray()) {
            return null;
        }
        String goalType = plan.path(GOAL_TYPE_FIELD).asText("");
        String beneficiaryProfileId = plan.path(BENEFICIARY_PROFILE_ID_FIELD).asText("");
        for (JsonNode goal : formulaGoals) {
            if (!goalType.equals(goal.path(GOAL_ID_FIELD).asText(""))) {
                continue;
            }
            if (YEAR_ONE_GOAL_TYPE.equals(goalType)) {
                if (beneficiaryProfileId.equals(goal.path(BENEFICIARY_PROFILE_ID_FIELD).asText(""))) {
                    return goal;
                }
            } else {
                return goal;
            }
        }
        return null;
    }

    private ObjectNode buildGoalDetailEntry(JsonNode plan, JsonNode matchedGoal) {
        String goalType = plan.path(GOAL_TYPE_FIELD).asText("");
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(GOAL_ID_FIELD, goalType);
        String beneficiaryProfileId = plan.path(BENEFICIARY_PROFILE_ID_FIELD).asText("");
        if (!beneficiaryProfileId.isEmpty()) {
            entry.put(BENEFICIARY_PROFILE_ID_FIELD, beneficiaryProfileId);
        }
        entry.put(OBJECTIVE_FIELD, plan.path(OBJECTIVE_FIELD).asText(""));
        if (plan.hasNonNull(TARGET_STATE_FIELD)) {
            entry.put(TARGET_STATE_FIELD, plan.path(TARGET_STATE_FIELD).asText(""));
        }
        entry.set(DETAIL_FIELD, plan.path(DETAIL_FIELD));
        entry.put(CURRENT_VALUE_FIELD, matchedGoal.path(CURRENT_VALUE_FIELD).asDouble(0.0));
        entry.put(TARGET_VALUE_FIELD, matchedGoal.path(TARGET_VALUE_FIELD).asDouble(0.0));
        entry.put(STATUS_FIELD, matchedGoal.path(STATUS_FIELD).asText(""));
        entry.put(UNIT_FIELD, matchedGoal.path(UNIT_FIELD).asText(""));

        ArrayNode milestonesArray = MAPPER.createArrayNode();
        for (JsonNode milestone : plan.path(MILESTONES_FIELD)) {
            milestonesArray.add(buildMilestoneDetailEntry(goalType, milestone, matchedGoal));
        }
        entry.set(MILESTONES_FIELD, milestonesArray);
        entry.set(RULES_FIELD, plan.path(RULES_FIELD));
        entry.set(TRIGGER_EVENTS_FIELD, plan.path(TRIGGER_EVENTS_FIELD));
        return entry;
    }

    /**
     * is_manual_checklist milestones keep their own admin-toggled is_achieved
     * unchanged (never recomputed here — that's the single-milestone PATCH endpoint's
     * job). Non-checklist milestones derive is_achieved from the matched goal's live
     * current_value vs. the milestone's own target_value, using the same
     * {@link #isGoalAchieved} per-goal-type direction lookup computeFormulaGoals()
     * already built — not a re-hardcoded exception.
     */
    private ObjectNode buildMilestoneDetailEntry(String goalType, JsonNode milestone, JsonNode matchedGoal) {
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(ID_FIELD, milestone.path(ID_FIELD).asText(""));
        entry.put(SEQUENCE_NO_FIELD, milestone.path(SEQUENCE_NO_FIELD).asInt(0));
        entry.put(LABEL_FIELD, milestone.path(LABEL_FIELD).asText(""));
        entry.put(SIGNIFICANCE_FIELD, milestone.path(SIGNIFICANCE_FIELD).asText(""));
        boolean isManualChecklist = milestone.path(IS_MANUAL_CHECKLIST_FIELD).asBoolean(false);
        entry.put(IS_MANUAL_CHECKLIST_FIELD, isManualChecklist);
        if (milestone.hasNonNull(TARGET_VALUE_FIELD)) {
            entry.put(TARGET_VALUE_FIELD, milestone.path(TARGET_VALUE_FIELD).asDouble(0.0));
        }

        boolean achieved;
        if (isManualChecklist) {
            achieved = milestone.path(IS_ACHIEVED_FIELD).asBoolean(false);
        } else {
            double currentValue = matchedGoal.path(CURRENT_VALUE_FIELD).asDouble(0.0);
            double milestoneTarget = milestone.path(TARGET_VALUE_FIELD).asDouble(0.0);
            achieved = isGoalAchieved(goalType, currentValue, milestoneTarget);
        }
        entry.put(IS_ACHIEVED_FIELD, achieved);
        return entry;
    }

    /**
     * Raw list, not a blended total (ADR-022, confirmed 2026-07-11) — payout_structure
     * is heterogeneous (lump sum / escalating income / sum-assured-at-maturity) and
     * collapsing it into one comparable figure needs real actuarial logic, out of
     * scope here. Only active policies are included.
     */
    private ArrayNode activeInsurancePolicies(UUID adminId) {
        ArrayNode result = MAPPER.createArrayNode();
        JsonNode policies;
        try {
            policies = wealthServiceClient.listInsurancePolicies(adminId).path(INSURANCE_POLICIES_FIELD);
        } catch (RuntimeException e) {
            AppLogger.info(
                    "ProjectionEngine: could not load insurance policies for admin %s, omitting from goal detail",
                    adminId);
            return result;
        }
        if (!policies.isArray()) {
            return result;
        }
        for (JsonNode policy : policies) {
            if (!policy.path(IS_ACTIVE_FIELD).asBoolean(false)) {
                continue;
            }
            result.add(buildInsurancePolicyDetailEntry(policy));
        }
        return result;
    }

    private ObjectNode buildInsurancePolicyDetailEntry(JsonNode policy) {
        ObjectNode entry = MAPPER.createObjectNode();
        entry.put(PROVIDER_FIELD, policy.path(PROVIDER_FIELD).asText(""));
        entry.put(POLICY_NAME_FIELD, policy.path(POLICY_NAME_FIELD).asText(""));
        entry.put(POLICY_TYPE_FIELD, policy.path(POLICY_TYPE_FIELD).asText(""));
        entry.put(PREMIUM_AMOUNT_FIELD, policy.path(PREMIUM_AMOUNT_FIELD).asDouble(0.0));
        entry.put(PREMIUM_FREQUENCY_FIELD, policy.path(PREMIUM_FREQUENCY_FIELD).asText(""));
        if (policy.hasNonNull(COVERAGE_AMOUNT_FIELD)) {
            entry.put(COVERAGE_AMOUNT_FIELD, policy.path(COVERAGE_AMOUNT_FIELD).asDouble(0.0));
        }
        entry.set(PAYOUT_STRUCTURE_FIELD, policy.path(PAYOUT_STRUCTURE_FIELD));
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
