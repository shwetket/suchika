package com.suchika.gateway.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suchika.gateway.health.HealthServiceClient;
import com.suchika.gateway.household.HouseholdServiceClient;
import com.suchika.gateway.profile.ProfileServiceClient;
import com.suchika.gateway.wealth.WealthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Plain JUnit 5 unit test for ConsoleErrorAggregationService — no Quarkus
 * container needed, matching the VacationPlannerServiceTest style.
 */
class ConsoleErrorAggregationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    ProfileServiceClient profileServiceClient;
    @Mock
    WealthServiceClient wealthServiceClient;
    @Mock
    HealthServiceClient healthServiceClient;
    @Mock
    HouseholdServiceClient householdServiceClient;

    private ConsoleErrorAggregationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ConsoleErrorAggregationService(
                profileServiceClient, wealthServiceClient, healthServiceClient, householdServiceClient);
    }

    @Test
    void aggregate_happyPath_combinesAllFourDomains() throws Exception {
        when(profileServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[{\"error_code\":\"NOT_FOUND\"}]"));
        when(wealthServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[]"));
        when(healthServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[]"));
        when(householdServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[]"));

        JsonNode result = service.aggregate(null, 50);

        assertEquals(1, result.get("profile").size());
        assertEquals(0, result.get("wealth").size());
        assertEquals(0, result.get("health").size());
        assertEquals(0, result.get("household").size());
    }

    @Test
    void aggregate_oneDomainDown_stillReturnsOthers() throws Exception {
        when(profileServiceClient.listErrors(any(), any())).thenThrow(new RuntimeException("connection refused"));
        when(wealthServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[{\"error_code\":\"BAD_REQUEST\"}]"));
        when(healthServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[]"));
        when(householdServiceClient.listErrors(any(), any())).thenReturn(MAPPER.readTree("[]"));

        JsonNode result = service.aggregate(null, 50);

        assertEquals(1, result.get("profile").size());
        assertTrue(result.get("profile").get(0).has("error"));
        assertEquals(1, result.get("wealth").size());
    }
}
