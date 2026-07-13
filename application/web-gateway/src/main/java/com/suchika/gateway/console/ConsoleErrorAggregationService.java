package com.suchika.gateway.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.profile.ProfileServiceClient;
import com.suchika.gateway.wealth.WealthServiceClient;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.function.Supplier;

/**
 * Fans out {@code GET /v1/errors} to each of the four domain services and
 * combines the results into one payload (Phase 4 Application Console,
 * ADR-023) — same "N independent per-service REST calls, combined in gateway
 * application memory" aggregation pattern ADR-013 already established for
 * {@code ProjectionCalculationEngine}: no cross-domain SQL join (ADR-003),
 * each call is a single self-contained domain request.
 *
 * <p>A domain that is down or errors out contributes an empty array rather
 * than failing the whole aggregation — the Console's entire purpose is to
 * stay useful when some services are unhealthy.
 */
@ApplicationScoped
public class ConsoleErrorAggregationService {

    private static final String ERROR_FIELD = "error";

    private final ProfileServiceClient profileServiceClient;
    private final WealthServiceClient wealthServiceClient;
    private final HealthServiceClient healthServiceClient;
    private final HouseholdServiceClient householdServiceClient;

    @Inject
    public ConsoleErrorAggregationService(
            @RestClient ProfileServiceClient profileServiceClient,
            @RestClient WealthServiceClient wealthServiceClient,
            @RestClient HealthServiceClient healthServiceClient,
            @RestClient HouseholdServiceClient householdServiceClient) {
        this.profileServiceClient = profileServiceClient;
        this.wealthServiceClient = wealthServiceClient;
        this.healthServiceClient = healthServiceClient;
        this.householdServiceClient = householdServiceClient;
    }

    public JsonNode aggregate(String since, Integer limit) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("profile", fetch("profile", () -> profileServiceClient.listErrors(since, limit)));
        result.set("wealth", fetch("wealth", () -> wealthServiceClient.listErrors(since, limit)));
        result.set("health", fetch("health", () -> healthServiceClient.listErrors(since, limit)));
        result.set("household", fetch("household", () -> householdServiceClient.listErrors(since, limit)));
        return result;
    }

    private JsonNode fetch(String domain, Supplier<JsonNode> call) {
        try {
            JsonNode result = call.get();
            return result != null ? result : JsonNodeFactory.instance.arrayNode();
        } catch (RuntimeException e) {
            AppLogger.warn("Console: failed to fetch errors from %s: %s", domain, e.getMessage());
            ArrayNode fallback = JsonNodeFactory.instance.arrayNode();
            ObjectNode errorEntry = JsonNodeFactory.instance.objectNode();
            errorEntry.put(ERROR_FIELD, "Could not reach " + domain + " service: " + e.getMessage());
            fallback.add(errorEntry);
            return fallback;
        }
    }
}
