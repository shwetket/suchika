package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.RecordVitalReadingRequest;
import com.suchika.health.adapters.http.dto.UpdateVitalReadingRequest;
import com.suchika.health.adapters.http.dto.VitalReadingResponse;
import com.suchika.health.adapters.services.VitalReadingService;
import com.suchika.health.ports.output.VitalReadingRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * True end-to-end integration test: record a vital reading then PATCH-update it,
 * exercising the real HTTP resource, real VitalReadingService, real Panache repository,
 * and real PostgreSQL. Contrast with VitalReadingResourceTest, which constructs the
 * Resource with a hand-written stub use-case and never touches the DB. This test wires
 * the real, DI-managed service and repository, then calls Resource methods directly
 * (same calling convention as the existing VitalReadingResourceTest).
 * <p>
 * Requires a running local PostgreSQL (app_db) with the health + profile schemas migrated.
 * Uses the seeded profile (R__seed_profile_test_data.sql) as the FK target for profile_id,
 * and creates its own VitalReading rows so it never assumes an empty table or touches
 * the seeded BLOOD_PRESSURE reading from R__seed_health_test_data.sql.
 * Runs in a transaction that is rolled back on completion (@TestTransaction).
 */
@QuarkusTest
@TestTransaction
class VitalReadingCreateUpdateIT {

    // Seeded in R__seed_profile_test_data.sql — guaranteed to exist when profile service has run
    private static final UUID SEED_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Inject
    VitalReadingRepository repository;

    private VitalReadingResource resource;

    @BeforeEach
    void setUp() {
        VitalReadingService service = new VitalReadingService(repository);
        resource = new VitalReadingResource(service);
    }

    @Test
    void recordVitalReading_thenPatchUpdate_persistsChangesInRealDb() {
        RecordVitalReadingRequest createRequest = new RecordVitalReadingRequest();
        createRequest.profileId = SEED_PROFILE_ID;
        createRequest.vitalType = "WEIGHT";
        createRequest.readingDate = LocalDate.of(2026, Month.JULY, 1);
        createRequest.valuePrimary = new BigDecimal("70.5");
        createRequest.unit = "kg";
        createRequest.notes = "IT create test";

        Response createResponse = resource.recordVitalReading(createRequest);
        assertEquals(201, createResponse.getStatus());
        VitalReadingResponse created = (VitalReadingResponse) createResponse.getEntity();
        assertEquals("WEIGHT", created.vitalType);
        assertNotNull(created.id);

        // Update only value_primary and notes — reading_date/unit should be preserved
        UpdateVitalReadingRequest updateRequest = new UpdateVitalReadingRequest();
        updateRequest.valuePrimary = new BigDecimal("71.2");
        updateRequest.notes = "IT update test";

        VitalReadingResponse updated = resource.update(created.id, updateRequest);
        assertEquals(created.id, updated.id);
        assertEquals(0, new BigDecimal("71.2").compareTo(updated.valuePrimary));
        assertEquals("IT update test", updated.notes);
        assertEquals(LocalDate.of(2026, Month.JULY, 1), updated.readingDate);
        assertEquals("kg", updated.unit);

        // Round-trip verify via GET, proving the update landed in real Postgres
        VitalReadingResponse fetched = resource.getById(created.id);
        assertEquals(0, new BigDecimal("71.2").compareTo(fetched.valuePrimary));
        assertEquals("IT update test", fetched.notes);
    }

    @Test
    void recordVitalReading_bloodPressureMissingDiastolic_throwsBadRequest() {
        RecordVitalReadingRequest request = new RecordVitalReadingRequest();
        request.profileId = SEED_PROFILE_ID;
        request.vitalType = "BLOOD_PRESSURE";
        request.readingDate = LocalDate.of(2026, Month.JULY, 1);
        request.valuePrimary = new BigDecimal("120.0");
        request.unit = "mmHg";

        assertThrows(BadRequestException.class, () -> resource.recordVitalReading(request));
    }

    @Test
    void updateVitalReading_unknownId_throwsNotFound() {
        UpdateVitalReadingRequest request = new UpdateVitalReadingRequest();
        request.valuePrimary = new BigDecimal("99.9");

        assertThrows(NotFoundException.class, () -> resource.update(UUID.randomUUID(), request));
    }
}
