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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
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
     * Refreshes all nine snapshot keys for the given profile.
     * Each compute step is independent; a failure in one does not block the others
     * (Bug 3 fix — see Epic 8 Phase 4 / OpenQuestions.md Q4).
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
        payload.put("total_monthly_emi", totalEmi);
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
        tiers.put("LIQUID", liquidTotal);
        tiers.put("SEMI_LIQUID", semiLiquidTotal);
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
                case "LIQUID" -> totals[0] += balance;
                case "SEMI_LIQUID" -> totals[1] += balance;
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
