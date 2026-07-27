package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.ListVitalReadingsResponse;
import com.suchika.health.adapters.http.dto.RecordVitalReadingRequest;
import com.suchika.health.adapters.http.dto.UpdateVitalReadingRequest;
import com.suchika.health.adapters.http.dto.VitalReadingResponse;
import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.input.PagedVitalReadings;
import com.suchika.health.ports.input.UpdateVitalReadingCommand;
import com.suchika.health.ports.input.VitalReadingUseCase;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
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
        useCase.pagedReadingsToReturn = new PagedVitalReadings(List.of(buildReading()), 1);

        Response response = resource.listVitals(PROFILE_ID, "WEIGHT", null, null);

        assertEquals(200, response.getStatus());
        ListVitalReadingsResponse body = (ListVitalReadingsResponse) response.getEntity();
        assertEquals(1, body.vitalReadings.size());
        assertEquals(VitalType.WEIGHT, useCase.lastListVitalType);
        assertEquals(1, body.totalSize);
    }

    @Test
    void listVitals_invalidVitalType_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVitals(PROFILE_ID, "NOT_A_TYPE", null, null));
    }

    @Test
    void listVitals_defaultsPageAndSizeWhenNotProvided() {
        useCase.pagedReadingsToReturn = new PagedVitalReadings(List.of(buildReading()), 1);

        Response response = resource.listVitals(PROFILE_ID, null, null, null);

        ListVitalReadingsResponse body = (ListVitalReadingsResponse) response.getEntity();
        assertEquals(0, body.page);
        assertEquals(50, body.size);
        assertEquals(0, useCase.lastListPage);
        assertEquals(50, useCase.lastListSize);
    }

    @Test
    void listVitals_honorsExplicitPageAndSize() {
        useCase.pagedReadingsToReturn = new PagedVitalReadings(List.of(buildReading()), 120);

        Response response = resource.listVitals(PROFILE_ID, null, 2, 25);

        ListVitalReadingsResponse body = (ListVitalReadingsResponse) response.getEntity();
        assertEquals(2, body.page);
        assertEquals(25, body.size);
        assertEquals(120, body.totalSize);
        assertEquals(2, useCase.lastListPage);
        assertEquals(25, useCase.lastListSize);
    }

    @Test
    void listVitals_negativePage_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVitals(PROFILE_ID, null, -1, null));
    }

    @Test
    void listVitals_sizeExceedsMax_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVitals(PROFILE_ID, null, null, 500));
    }

    @Test
    void listVitals_sizeZero_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVitals(PROFILE_ID, null, null, 0));
    }

    @Test
    void listVitals_missingProfileId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVitals(null, "WEIGHT", null, null));
    }

    @Test
    void recordVitalReading_returns201_withCreatedReading() {
        RecordVitalReadingRequest request = new RecordVitalReadingRequest();
        request.profileId = PROFILE_ID;
        request.vitalType = "WEIGHT";
        request.readingDate = LocalDate.of(2026, Month.JUNE, 1);
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
                .readingDate(LocalDate.of(2026, Month.JUNE, 1))
                .valuePrimary(BigDecimal.valueOf(70))
                .unit("kg")
                .build();
    }

    static class StubUseCase implements VitalReadingUseCase {
        List<VitalReading> readingsToReturn = List.of();
        PagedVitalReadings pagedReadingsToReturn = new PagedVitalReadings(List.of(), 0);
        VitalReading readingToReturn;

        VitalType lastListVitalType;
        VitalType lastRecordVitalType;
        UUID lastUpdateId;
        UpdateVitalReadingCommand lastUpdateCommand;
        UUID lastDeleteId;
        Integer lastListPage;
        Integer lastListSize;

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
        public PagedVitalReadings listByProfilePaginated(UUID profileId, VitalType vitalType, int page, int size) {
            lastListVitalType = vitalType;
            lastListPage = page;
            lastListSize = size;
            return pagedReadingsToReturn;
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
