package com.suchika.health.adapters.http;

import com.suchika.health.adapters.http.dto.CreateDoctorVisitRequest;
import com.suchika.health.adapters.http.dto.DoctorVisitResponse;
import com.suchika.health.adapters.http.dto.ListDoctorVisitsResponse;
import com.suchika.health.adapters.http.dto.UpdateDoctorVisitRequest;
import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.input.CreateDoctorVisitCommand;
import com.suchika.health.ports.input.DoctorVisitUseCase;
import com.suchika.health.ports.input.PagedDoctorVisits;
import com.suchika.health.ports.input.UpdateDoctorVisitCommand;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorVisitResourceTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();

    private DoctorVisitResource resource;
    private StubUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new DoctorVisitResource(useCase);
    }

    @Test
    void listVisits_returns200_withVisitList() {
        useCase.pagedVisitsToReturn = new PagedDoctorVisits(List.of(buildVisit()), 1);

        Response response = resource.listVisits(PROFILE_ID, null, null, null, null);

        assertEquals(200, response.getStatus());
        ListDoctorVisitsResponse body = (ListDoctorVisitsResponse) response.getEntity();
        assertEquals(1, body.doctorVisits.size());
        assertEquals("Dr. Rao", body.doctorVisits.get(0).doctorName);
        assertEquals(1, body.totalSize);
    }

    @Test
    void listVisits_passesDateRangeThroughToUseCase() {
        useCase.pagedVisitsToReturn = new PagedDoctorVisits(List.of(buildVisit()), 1);

        resource.listVisits(PROFILE_ID, "2026-01-01", "2026-06-30", null, null);

        assertEquals(LocalDate.of(2026, Month.JANUARY, 1), useCase.lastListFrom);
        assertEquals(LocalDate.of(2026, Month.JUNE, 30), useCase.lastListTo);
    }

    @Test
    void listVisits_invalidFromDate_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVisits(PROFILE_ID, "not-a-date", null, null, null));
    }

    @Test
    void listVisits_defaultsPageAndSizeWhenNotProvided() {
        useCase.pagedVisitsToReturn = new PagedDoctorVisits(List.of(buildVisit()), 1);

        Response response = resource.listVisits(PROFILE_ID, null, null, null, null);

        ListDoctorVisitsResponse body = (ListDoctorVisitsResponse) response.getEntity();
        assertEquals(0, body.page);
        assertEquals(50, body.size);
        assertEquals(0, useCase.lastListPage);
        assertEquals(50, useCase.lastListSize);
    }

    @Test
    void listVisits_honorsExplicitPageAndSize() {
        useCase.pagedVisitsToReturn = new PagedDoctorVisits(List.of(buildVisit()), 120);

        Response response = resource.listVisits(PROFILE_ID, null, null, 2, 25);

        ListDoctorVisitsResponse body = (ListDoctorVisitsResponse) response.getEntity();
        assertEquals(2, body.page);
        assertEquals(25, body.size);
        assertEquals(120, body.totalSize);
        assertEquals(2, useCase.lastListPage);
        assertEquals(25, useCase.lastListSize);
    }

    @Test
    void listVisits_negativePage_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVisits(PROFILE_ID, null, null, -1, null));
    }

    @Test
    void listVisits_sizeExceedsMax_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVisits(PROFILE_ID, null, null, null, 500));
    }

    @Test
    void listVisits_sizeZero_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVisits(PROFILE_ID, null, null, null, 0));
    }

    @Test
    void listVisits_missingProfileId_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> resource.listVisits(null, null, null, null, null));
    }

    @Test
    void create_returns201_withCreatedVisit() {
        CreateDoctorVisitRequest request = new CreateDoctorVisitRequest();
        request.profileId = PROFILE_ID;
        request.fromDate = LocalDate.of(2026, Month.JUNE, 1);
        request.visitedDoctor = true;
        request.doctorName = "Dr. Rao";
        useCase.visitToReturn = buildVisit();

        Response response = resource.create(request);

        assertEquals(201, response.getStatus());
        assertEquals(PROFILE_ID, useCase.lastCreateCommand.profileId());
        assertTrue(useCase.lastCreateCommand.visitedDoctor());
        DoctorVisitResponse body = (DoctorVisitResponse) response.getEntity();
        assertEquals("Dr. Rao", body.doctorName);
    }

    @Test
    void create_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.create(null));
    }

    @Test
    void create_visitedDoctorNull_defaultsToFalse() {
        CreateDoctorVisitRequest request = new CreateDoctorVisitRequest();
        request.profileId = PROFILE_ID;
        request.fromDate = LocalDate.of(2026, Month.JUNE, 1);
        useCase.visitToReturn = buildVisit();

        resource.create(request);

        assertEquals(false, useCase.lastCreateCommand.visitedDoctor());
    }

    @Test
    void create_visitedDoctorExplicitlyFalse_staysFalse() {
        CreateDoctorVisitRequest request = new CreateDoctorVisitRequest();
        request.profileId = PROFILE_ID;
        request.fromDate = LocalDate.of(2026, Month.JUNE, 1);
        request.visitedDoctor = Boolean.FALSE;
        useCase.visitToReturn = buildVisit();

        resource.create(request);

        assertEquals(false, useCase.lastCreateCommand.visitedDoctor());
    }

    @Test
    void getById_returnsVisitResponse() {
        useCase.visitToReturn = buildVisit();

        DoctorVisitResponse response = resource.getById(VISIT_ID);

        assertEquals("Dr. Rao", response.doctorName);
    }

    @Test
    void update_returnsUpdatedVisit() {
        UpdateDoctorVisitRequest request = new UpdateDoctorVisitRequest();
        request.doctorName = "Dr. Iyer";
        request.diagnosis = "Flu";
        useCase.visitToReturn = buildVisit();

        DoctorVisitResponse response = resource.update(VISIT_ID, request);

        assertEquals("Dr. Rao", response.doctorName);
        assertEquals(VISIT_ID, useCase.lastUpdateId);
        assertEquals("Dr. Iyer", useCase.lastUpdateCommand.doctorName());
        assertEquals("Flu", useCase.lastUpdateCommand.diagnosis());
    }

    @Test
    void update_nullBody_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.update(VISIT_ID, null));
    }

    @Test
    void delete_returns204() {
        Response response = resource.delete(VISIT_ID);

        assertEquals(204, response.getStatus());
        assertEquals(VISIT_ID, useCase.lastDeleteId);
    }

    private DoctorVisit buildVisit() {
        return DoctorVisit.builder()
                .id(VISIT_ID)
                .profileId(PROFILE_ID)
                .fromDate(LocalDate.of(2026, Month.JUNE, 1))
                .visitedDoctor(true)
                .doctorName("Dr. Rao")
                .build();
    }

    static class StubUseCase implements DoctorVisitUseCase {
        List<DoctorVisit> visitsToReturn = List.of();
        PagedDoctorVisits pagedVisitsToReturn = new PagedDoctorVisits(List.of(), 0);
        DoctorVisit visitToReturn;

        CreateDoctorVisitCommand lastCreateCommand;
        UUID lastUpdateId;
        UpdateDoctorVisitCommand lastUpdateCommand;
        UUID lastDeleteId;
        LocalDate lastListFrom;
        LocalDate lastListTo;
        Integer lastListPage;
        Integer lastListSize;

        @Override
        public DoctorVisit create(CreateDoctorVisitCommand command) {
            lastCreateCommand = command;
            return visitToReturn;
        }

        @Override
        public DoctorVisit getById(UUID id) {
            return visitToReturn;
        }

        @Override
        public List<DoctorVisit> listByProfile(UUID profileId, LocalDate from, LocalDate to) {
            lastListFrom = from;
            lastListTo = to;
            return visitsToReturn;
        }

        @Override
        public PagedDoctorVisits listByProfilePaginated(UUID profileId, LocalDate from, LocalDate to,
                                                          int page, int size) {
            lastListFrom = from;
            lastListTo = to;
            lastListPage = page;
            lastListSize = size;
            return pagedVisitsToReturn;
        }

        @Override
        public DoctorVisit update(UUID id, UpdateDoctorVisitCommand command) {
            lastUpdateId = id;
            lastUpdateCommand = command;
            return visitToReturn;
        }

        @Override
        public void delete(UUID id) {
            lastDeleteId = id;
        }
    }
}
