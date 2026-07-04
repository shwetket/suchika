package com.suchika.gateway.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * True cross-domain integration test: create an admin+profile (profile service),
 * create an account+transaction (wealth service), post a vital reading (health service),
 * trigger POST /v1/projections/refresh/{profileId} (web-gateway), then GET the dashboard
 * and assert real computed values (net worth, vitals summary) reflect the real data just
 * written across three separate domain databases.
 * <p>
 * This is the automated equivalent of the manual curl-based QA pass performed this session —
 * it was previously verified by hand only and never captured as a regression test.
 * <p>
 * Deliberately NOT a @QuarkusTest: the gateway's @QuarkusTest harness mocks every
 * @RestClient (ADR-011, see ProjectionResourceTest/ProfileGatewayResourceTest) and disables
 * its own datasource in the test profile, so it is structurally incapable of proving a real
 * multi-service round trip. Each domain service's own @QuarkusTest datasource also points at
 * the same shared dev Postgres and would TRUNCATE CASCADE via R__seed_*_test_data.sql on
 * every run (see Q34 in documents/OpenQuestions.md) — unsafe while services are live and
 * being manually tested. This test instead drives the already-running dev-mode services
 * (profile:8081, wealth:8082, health:8083, gateway:8080) purely over HTTP, exactly like a
 * human running curl commands — no Flyway migration is triggered, no schema is touched
 * directly, and every row this test creates is uniquely named so it never assumes an
 * empty table and never collides with concurrent manual testing against the same services.
 * <p>
 * Skips (rather than fails) if the gateway is not reachable, so it does not break CI runs
 * where the dev-mode services are not started — this is the accepted trade-off documented
 * in Q34 until a dedicated ephemeral-stack CI job exists.
 */
class CrossDomainDashboardE2ETest {

    private static final String PROFILE_BASE = "http://localhost:8081";
    private static final String WEALTH_BASE = "http://localhost:8082";
    private static final String HEALTH_BASE = "http://localhost:8083";
    private static final String GATEWAY_BASE = "http://localhost:8080";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createAdminProfileAccountTransactionVital_refreshProjections_dashboardReflectsRealData() throws Exception {
        assumeTrue(isReachable(GATEWAY_BASE + "/v1/admins"), "web-gateway not reachable on :8080 — skipping live cross-domain E2E test");
        assumeTrue(isReachable(PROFILE_BASE + "/v1/admins"), "profile service not reachable on :8081 — skipping live cross-domain E2E test");
        assumeTrue(isReachable(WEALTH_BASE + "/v1/accounts"), "wealth service not reachable on :8082 — skipping live cross-domain E2E test");
        assumeTrue(isReachable(HEALTH_BASE + "/v1/vitals?profile_id=00000000-0000-0000-0000-000000000002"), "health service not reachable on :8083 — skipping live cross-domain E2E test");

        String uniqueSuffix = String.valueOf(System.nanoTime());
        String uniqueEmail = "e2e-dashboard-" + uniqueSuffix + "@test.com";

        // 1. Create admin (profile service, direct — gateway also proxies this but we exercise
        //    the domain service directly here since the gateway path is covered by ProfileGatewayResourceTest)
        JsonNode adminResponse = postJson(PROFILE_BASE + "/v1/admins",
                "{\"display_name\":\"E2E Dashboard Admin\",\"email_address\":\"" + uniqueEmail + "\"}", 201);
        String adminId = adminResponse.path("admin_id").asText();
        assertNotNull(adminId);

        // 2. Create SELF profile for that admin (profile service)
        JsonNode profileResponse = postJson(PROFILE_BASE + "/v1/profiles",
                "{"
                        + "\"admin_id\":\"" + adminId + "\","
                        + "\"full_name\":\"E2E Dashboard Member\","
                        + "\"dob\":\"1990-05-20\","
                        + "\"relation_to_admin\":\"SELF\""
                        + "}", 201);
        String profileId = profileResponse.path("profile_id").asText();
        assertNotNull(profileId);

        // 3. Create an account for that profile (wealth service) with a known opening balance
        JsonNode accountResponse = postJson(WEALTH_BASE + "/v1/accounts?profile_id=" + profileId,
                "{"
                        + "\"account_name\":\"E2E Dashboard Account\","
                        + "\"account_type\":\"SAVINGS\","
                        + "\"institution_name\":\"E2E Test Bank\","
                        + "\"opening_balance\":10000.00"
                        + "}", 201);
        String accountId = accountResponse.path("account_id").asText();
        assertNotNull(accountId);

        // 4. Add a manual CREDIT transaction of 5000 -> expected balance 15000
        postJson(WEALTH_BASE + "/v1/accounts/" + accountId + "/transactions?profile_id=" + profileId,
                "{"
                        + "\"txn_date\":\"2026-07-01\","
                        + "\"amount\":5000.00,"
                        + "\"txn_type\":\"CREDIT\","
                        + "\"description\":\"E2E dashboard salary credit\""
                        + "}", 201);

        // 5. Post a vital reading for that profile (health service)
        postJson(HEALTH_BASE + "/v1/vitals",
                "{"
                        + "\"profile_id\":\"" + profileId + "\","
                        + "\"vital_type\":\"WEIGHT\","
                        + "\"reading_date\":\"2026-07-01\","
                        + "\"value_primary\":72.5,"
                        + "\"unit\":\"kg\""
                        + "}", 201);

        // 6. Trigger the projection refresh through the gateway
        HttpResponse<String> refreshResponse = send(
                HttpRequest.newBuilder(URI.create(GATEWAY_BASE + "/v1/projections/refresh/" + profileId))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header("Content-Type", "application/json")
                        .build());
        assertEquals(200, refreshResponse.statusCode(), "refresh should succeed: " + refreshResponse.body());

        // 7. GET the dashboard and assert the real computed values reflect what we just wrote
        HttpResponse<String> dashboardResponse = send(
                HttpRequest.newBuilder(URI.create(GATEWAY_BASE + "/v1/projections/dashboard/" + profileId))
                        .GET()
                        .build());
        assertEquals(200, dashboardResponse.statusCode());

        JsonNode dashboard = mapper.readTree(dashboardResponse.body());
        JsonNode snapshots = dashboard.path("snapshots");
        assertTrue(snapshots.isArray() && snapshots.size() > 0, "dashboard should have at least one snapshot");

        JsonNode netWorthSnapshot = findSnapshot(snapshots, "WEALTH_NET_WORTH");
        assertNotNull(netWorthSnapshot, "expected a WEALTH_NET_WORTH snapshot after refresh");
        JsonNode netWorthPayload = mapper.readTree(netWorthSnapshot.path("payload").asText());
        // opening_balance 10000 + CREDIT 5000 = 15000, and this profile has exactly this one account
        assertEquals(15000.0, netWorthPayload.path("net_worth").asDouble(), 0.001);
        assertEquals(1, netWorthPayload.path("account_count").asInt());

        JsonNode vitalsSnapshot = findSnapshot(snapshots, "HEALTH_VITALS_SUMMARY");
        assertNotNull(vitalsSnapshot, "expected a HEALTH_VITALS_SUMMARY snapshot after refresh");
        JsonNode vitalsPayload = mapper.readTree(vitalsSnapshot.path("payload").asText());
        JsonNode vitals = vitalsPayload.path("vitals");
        assertTrue(vitals.isArray());
        boolean hasWeightReading = false;
        for (JsonNode vital : vitals) {
            if ("WEIGHT".equals(vital.path("vital_type").asText())) {
                assertEquals(72.5, vital.path("value_primary").asDouble(), 0.001);
                hasWeightReading = true;
            }
        }
        assertTrue(hasWeightReading, "expected the WEIGHT reading just posted to appear in the vitals summary");
    }

    private JsonNode findSnapshot(JsonNode snapshots, String key) {
        for (JsonNode snapshot : snapshots) {
            if (key.equals(snapshot.path("snapshot_key").asText())) {
                return snapshot;
            }
        }
        return null;
    }

    private JsonNode postJson(String url, String jsonBody, int expectedStatus) throws IOException, InterruptedException {
        HttpResponse<String> response = send(
                HttpRequest.newBuilder(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .header("Content-Type", "application/json")
                        .build());
        assertEquals(expectedStatus, response.statusCode(), "POST " + url + " failed: " + response.body());
        return mapper.readTree(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * No smallrye-health endpoint is wired up in any service (verified: /q/health returns 404
     * everywhere, and the gateway also 404s on /q/openapi), so reachability is probed against
     * each service's own real, always-present listing route instead of an ops endpoint.
     */
    private boolean isReachable(String probeUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(probeUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }
}
