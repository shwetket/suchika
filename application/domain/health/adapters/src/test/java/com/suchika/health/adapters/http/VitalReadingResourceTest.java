package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.ListVitalReadingsResponse;
import com.suchika.health.adapters.http.dto.RecordVitalReadingRequest;
import com.suchika.health.adapters.http.dto.UpdateVitalReadingRequest;
import com.suchika.health.adapters.http.dto.VitalReadingResponse;
import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.input.UpdateVitalReadingCommand;
import com.suchika.health.ports.input.VitalReadingUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VitalReadingResourceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID READING_ID = UUID.randomUUID();

    private VitalReadingResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new VitalReadingResource(useCase);
    }

    @Test
    void listVitals_returns200_withReadingList() {
        useCase.readingsToReturn = List.of(buildReading());

        Response response = resource.listVitals(PROFILE_ID, "WEIGHT");

        assertEquals(200, response.getStatus());
        ListVitalReadingsResponse body = (ListVitalReadingsResponse) response.getEntity();
        assertEquals(1, body.vitalReadings.size());
        assertEquals(VitalType.WEIGHT, useCase.lastListVitalType);
    }

    @Test
    void listVitals_invalidVitalType_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.listVitals(PROFILE_ID, "NOT_A_TYPE"));
    }

    @Test
    void recordVitalReading_returns201_withCreatedReading() {
        RecordVitalReadingRequest request = new RecordVitalReadingRequest();
        request.profileId = PROFILE_ID;
        request.vitalType = "WEIGHT";
        request.readingDate = LocalDate.of(2026, 6, 1);
        request.valuePrimary = BigDecimal.valueOf(70);
        request.unit = "kg";
        useCase.readingToReturn = buildReading();

        Response response = resource.recordVitalReading(request);

        assertEquals(201, response.getStatus());
        assertEquals(VitalType.WEIGHT, useCase.lastRecordVitalType);
        VitalReadingResponse body = (VitalReadingResponse) response.getEntity();
        assertEquals("kg", body.unit);
    }

    @Test
    void recordVitalReading_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.recordVitalReading(null));
    }

    @Test
    void recordVitalReading_invalidVitalType_throwsBadRequest() {
        RecordVitalReadingRequest request = new RecordVitalReadingRequest();
        request.vitalType = "NOT_A_TYPE";
        assertThrows(BadRequestException.class, () -> resource.recordVitalReading(request));
    }

    @Test
    void getById_returnsReadingResponse() {
        useCase.readingToReturn = buildReading();

        VitalReadingResponse response = resource.getById(READING_ID);

        assertEquals("kg", response.unit);
    }

    @Test
    void update_returnsUpdatedReading() {
        UpdateVitalReadingRequest request = new UpdateVitalReadingRequest();
        request.valuePrimary = BigDecimal.valueOf(72);
        request.notes = "post-workout";
        useCase.readingToReturn = buildReading();

        VitalReadingResponse response = resource.update(READING_ID, request);

        assertEquals("kg", response.unit);
        assertEquals(READING_ID, useCase.lastUpdateId);
        assertEquals(BigDecimal.valueOf(72), useCase.lastUpdateCommand.valuePrimary());
        assertEquals("post-workout", useCase.lastUpdateCommand.notes());
    }

    @Test
    void update_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.update(READING_ID, null));
    }

    @Test
    void delete_returns204() {
        Response response = resource.delete(READING_ID);

        assertEquals(204, response.getStatus());
        assertEquals(READING_ID, useCase.lastDeleteId);
    }

    private VitalReading buildReading() {
        return VitalReading.builder()
                .id(READING_ID)
                .profileId(PROFILE_ID)
                .vitalType(VitalType.WEIGHT)
                .readingDate(LocalDate.of(2026, 6, 1))
                .valuePrimary(BigDecimal.valueOf(70))
                .unit("kg")
                .build();
    }

    static class StubUseCase implements VitalReadingUseCase {
        List<VitalReading> readingsToReturn = List.of();
        VitalReading readingToReturn;

        VitalType lastListVitalType;
        VitalType lastRecordVitalType;
        UUID lastUpdateId;
        UpdateVitalReadingCommand lastUpdateCommand;
        UUID lastDeleteId;

        @Override
        public VitalReading recordReading(UUID profileId, VitalType vitalType, LocalDate readingDate,
                                           BigDecimal valuePrimary, BigDecimal valueSecondary,
                                           String unit, String notes) {
            lastRecordVitalType = vitalType;
            return readingToReturn;
        }

        @Override
        public VitalReading getById(UUID id) {
            return readingToReturn;
        }

        @Override
        public List<VitalReading> listByProfile(UUID profileId, VitalType vitalType) {
            lastListVitalType = vitalType;
            return readingsToReturn;
        }

        @Override
        public VitalReading update(UUID id, UpdateVitalReadingCommand command) {
            lastUpdateId = id;
            lastUpdateCommand = command;
            return readingToReturn;
        }

        @Override
        public void delete(UUID id) {
            lastDeleteId = id;
        }
    }
}
