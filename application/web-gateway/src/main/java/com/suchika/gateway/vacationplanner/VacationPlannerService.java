package com.suchika.gateway.vacationplanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.projection.DashboardSnapshotDto;
import com.suchika.gateway.projection.DashboardSnapshotRepository;
import com.suchika.gateway.projection.SnapshotKey;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

/**
 * Vacation Planner (v0.5 Phase 2) — a gateway-native cross-domain compute, not a
 * proxy for a single domain resource. Composes wealth data only; no calendar
 * lookup is needed since the trip dates are supplied directly by the caller.
 *
 * <p>Two checks, returned together:
 * <ul>
 *   <li>Budget check — trip cost against the family's LIQUID tier balance, read
 *       from the already-computed {@code WEALTH_LIQUIDITY_TIERS_FAMILY} snapshot
 *       (Epic 8 Phase 3). Requires the dashboard to have been refreshed at least
 *       once; this is a read of existing gateway state, not a new computation.</li>
 *   <li>Asset compliance — vehicle PUC/insurance expiry (Q29: read directly from
 *       {@code physical_asset.metadata} JSONB, no schema promotion) checked
 *       against the trip end date, so a vehicle must stay compliant through the
 *       whole trip, not just its start.</li>
 * </ul>
 */
@ApplicationScoped
public class VacationPlannerService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String STATUS_FIELD = "status";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARNING = "WARNING";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String TIER_LIQUID = "LIQUID";
    private static final String VEHICLE_ASSET_TYPE = "VEHICLE";
    private static final String PHYSICAL_ASSETS_FIELD = "physical_assets";
    private static final String METADATA_FIELD = "metadata";
    private static final String ASSET_ID_FIELD = "asset_id";
    private static final String ASSET_NAME_FIELD = "asset_name";

    private final WealthServiceClient wealthServiceClient;
    private final DashboardSnapshotRepository snapshotRepository;

    @Inject
    public VacationPlannerService(
            @RestClient WealthServiceClient wealthServiceClient,
            DashboardSnapshotRepository snapshotRepository) {
        this.wealthServiceClient = wealthServiceClient;
        this.snapshotRepository = snapshotRepository;
    }

    public JsonNode checkBudget(UUID profileId, VacationPlannerRequest request) {
        if (profileId == null) {
            throw new BadRequestException("profile_id is required");
        }
        if (request == null || request.tripEndDate == null) {
            throw new BadRequestException("trip_end_date is required");
        }

        AppLogger.info("VacationPlanner: budget check for profile %s", profileId);

        ObjectNode response = MAPPER.createObjectNode();
        response.set("budget_check", buildBudgetCheck(profileId, request.tripCost));
        response.set("asset_compliance", buildAssetCompliance(profileId, request.tripEndDate));
        return response;
    }

    private ObjectNode buildBudgetCheck(UUID profileId, double tripCost) {
        ObjectNode node = MAPPER.createObjectNode();
        JsonNode tiersPayload = readSnapshot(profileId, SnapshotKey.WEALTH_LIQUIDITY_TIERS_FAMILY);
        if (tiersPayload == null) {
            node.put(STATUS_FIELD, STATUS_UNAVAILABLE);
            node.put("message", "Liquidity data not yet calculated — refresh the dashboard first");
            return node;
        }

        double liquidSavings = tiersPayload.path("tiers").path(TIER_LIQUID).asDouble(0.0);
        boolean sufficient = liquidSavings >= tripCost;

        node.put(STATUS_FIELD, sufficient ? STATUS_PASS : STATUS_WARNING);
        node.put("liquid_savings", liquidSavings);
        node.put("trip_cost", tripCost);
        node.put("shortfall", sufficient ? 0.0 : tripCost - liquidSavings);
        return node;
    }

    private ObjectNode buildAssetCompliance(UUID profileId, LocalDate tripEndDate) {
        ObjectNode node = MAPPER.createObjectNode();
        ArrayNode issues = MAPPER.createArrayNode();

        JsonNode assetsResponse = wealthServiceClient.listPhysicalAssets(
                VEHICLE_ASSET_TYPE, true, profileId.toString());
        JsonNode assetsArray = assetsResponse.path(PHYSICAL_ASSETS_FIELD);
        if (assetsArray.isArray()) {
            for (JsonNode asset : assetsArray) {
                addExpiryIssueIfAny(asset, "puc_expiry", "PUC", tripEndDate, issues);
                addExpiryIssueIfAny(asset, "insurance_expiry", "INSURANCE", tripEndDate, issues);
            }
        }

        node.put(STATUS_FIELD, issues.isEmpty() ? STATUS_PASS : STATUS_WARNING);
        node.set("issues", issues);
        return node;
    }

    private void addExpiryIssueIfAny(
            JsonNode asset, String metadataKey, String issueLabel, LocalDate tripEndDate, ArrayNode issues) {
        String expiryText = asset.path(METADATA_FIELD).path(metadataKey).asText("");
        if (expiryText.isBlank()) {
            return;
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiryText);
        } catch (DateTimeParseException e) {
            AppLogger.info("VacationPlanner: unparseable %s '%s' on asset %s, skipping",
                    metadataKey, expiryText, asset.path(ASSET_ID_FIELD).asText(""));
            return;
        }

        if (expiryDate.isBefore(tripEndDate)) {
            ObjectNode issue = MAPPER.createObjectNode();
            issue.put(ASSET_ID_FIELD, asset.path(ASSET_ID_FIELD).asText(""));
            issue.put(ASSET_NAME_FIELD, asset.path(ASSET_NAME_FIELD).asText(""));
            issue.put("issue_type", issueLabel.toUpperCase(Locale.ROOT) + "_EXPIRED");
            issue.put("expiry_date", expiryText);
            issues.add(issue);
        }
    }

    private JsonNode readSnapshot(UUID profileId, String snapshotKey) {
        for (DashboardSnapshotDto dto : snapshotRepository.findByProfileId(profileId)) {
            if (dto.getSnapshotKey().equals(snapshotKey)) {
                try {
                    return MAPPER.readTree(dto.getPayload());
                } catch (Exception e) {
                    AppLogger.error("VacationPlanner: failed to parse snapshot %s for profile %s", e,
                            snapshotKey, profileId);
                    return null;
                }
            }
        }
        return null;
    }
}
