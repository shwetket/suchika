package com.suchika.health.adapters.services;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.input.CreateDoctorVisitCommand;
import com.suchika.health.ports.input.PagedDoctorVisits;
import com.suchika.health.ports.input.UpdateDoctorVisitCommand;
import com.suchika.health.ports.output.DoctorVisitRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DoctorVisitServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, Month.JANUARY, 15);
    private static final LocalDate YESTERDAY = LocalDate.of(2024, Month.JANUARY, 14);

    private StubDoctorVisitRepository repository;
    private DoctorVisitService service;

    @BeforeEach
    void setUp() {
        repository = new StubDoctorVisitRepository();
        service = new DoctorVisitService(repository);
    }

    @Test
    void create_visited_doctor_happy_path() {
        CreateDoctorVisitCommand command = visitCmd(UUID.randomUUID(), true, "Dr. Sharma", "Apollo", "General");

        DoctorVisit visit = service.create(command);

        assertNotNull(visit);
        assertEquals("Dr. Sharma", visit.getDoctorName());
        assertTrue(visit.isVisitedDoctor());
    }

    @Test
    void create_illness_without_visit() {
        CreateDoctorVisitCommand command = noVisitCmd(UUID.randomUUID(), "Fever");

        DoctorVisit visit = service.create(command);

        assertFalse(visit.isVisitedDoctor());
        assertNull(visit.getDoctorName());
    }

    @Test
    void create_requires_doctor_name_when_visited() {
        CreateDoctorVisitCommand command = visitCmd(UUID.randomUUID(), true, null, null, null);

        assertThrows(BadRequestException.class, () -> service.create(command));
    }

    @Test
    void create_rejects_to_date_before_from_date() {
        CreateDoctorVisitCommand command = new CreateDoctorVisitCommand(
                UUID.randomUUID(), TODAY, YESTERDAY, false, null, null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> service.create(command));
    }

    @Test
    void create_rejects_null_profile_id() {
        CreateDoctorVisitCommand command = noVisitCmd(null, null);

        assertThrows(BadRequestException.class, () -> service.create(command));
    }

    @Test
    void create_rejects_null_from_date() {
        CreateDoctorVisitCommand command = new CreateDoctorVisitCommand(
                UUID.randomUUID(), null, null, false, null, null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> service.create(command));
    }

    @Test
    void update_partial_fields() {
        DoctorVisit created = service.create(visitCmd(UUID.randomUUID(), true, "Dr. Sharma", null, null));

        UpdateDoctorVisitCommand command = new UpdateDoctorVisitCommand(
                null, null, "Apollo Hospital", null, null, "Viral fever", null, null);
        DoctorVisit updated = service.update(created.getId(), command);

        assertEquals("Dr. Sharma", updated.getDoctorName());
        assertEquals("Apollo Hospital", updated.getHospitalName());
        assertEquals("Viral fever", updated.getDiagnosis());
    }

    @Test
    void update_rejects_to_date_before_from_date() {
        DoctorVisit created = service.create(noVisitCmd(UUID.randomUUID(), null));
        UUID createdId = created.getId();

        UpdateDoctorVisitCommand command = new UpdateDoctorVisitCommand(
                YESTERDAY, null, null, null, null, null, null, null);
        assertThrows(BadRequestException.class, () -> service.update(createdId, command));
    }

    @Test
    void getById_throws_not_found() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getById(unknownId));
    }

    @Test
    void listByProfile_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () -> service.listByProfile(null, null, null));
    }

    @Test
    void listByProfile_passesDateRangeThroughToRepo() {
        UUID profileId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate to = LocalDate.of(2026, Month.JUNE, 30);

        service.listByProfile(profileId, from, to);

        assertEquals(profileId, repository.lastProfileId);
        assertEquals(from, repository.lastFrom);
        assertEquals(to, repository.lastTo);
    }

    @Test
    void delete_throws_not_found_for_unknown_id() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.delete(unknownId));
    }

    @Test
    void listByProfilePaginated_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () -> service.listByProfilePaginated(null, null, null, 0, 10));
    }

    @Test
    void listByProfilePaginated_returnsRequestedPageAndTotalCount() {
        UUID profileId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            service.create(noVisitCmd(profileId, "visit " + i));
        }

        PagedDoctorVisits result = service.listByProfilePaginated(profileId, null, null, 1, 2);

        assertEquals(2, result.visits().size());
        assertEquals(5, result.totalCount());
    }

    @Test
    void listByProfilePaginated_passesDateRangeThroughToRepo() {
        UUID profileId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate to = LocalDate.of(2026, Month.JUNE, 30);

        service.listByProfilePaginated(profileId, from, to, 0, 10);

        assertEquals(profileId, repository.lastProfileId);
        assertEquals(from, repository.lastFrom);
        assertEquals(to, repository.lastTo);
    }

    // ── Test helpers ─────────────────────────────────────────────────────────

    private static CreateDoctorVisitCommand visitCmd(UUID profileId, boolean visitedDoctor,
            String doctorName, String hospital, String speciality) {
        return new CreateDoctorVisitCommand(
                profileId, TODAY, null, visitedDoctor, doctorName, hospital, speciality,
                null, null, null, null);
    }

    private static CreateDoctorVisitCommand noVisitCmd(UUID profileId, String symptoms) {
        return new CreateDoctorVisitCommand(
                profileId, TODAY, null, false, null, null, null,
                symptoms, null, null, null);
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubDoctorVisitRepository implements DoctorVisitRepository {

        private final Map<UUID, DoctorVisit> store = new LinkedHashMap<>();
        UUID lastProfileId;
        LocalDate lastFrom;
        LocalDate lastTo;

        @Override
        public DoctorVisit save(DoctorVisit visit) {
            UUID id = visit.getId() != null ? visit.getId() : UUID.randomUUID();
            DoctorVisit stored = DoctorVisit.builder()
                    .id(id).profileId(visit.getProfileId())
                    .fromDate(visit.getFromDate()).toDate(visit.getToDate())
                    .visitedDoctor(visit.isVisitedDoctor())
                    .doctorName(visit.getDoctorName()).hospitalName(visit.getHospitalName())
                    .speciality(visit.getSpeciality()).symptoms(visit.getSymptoms())
                    .diagnosis(visit.getDiagnosis()).notes(visit.getNotes())
                    .followUpDate(visit.getFollowUpDate())
                    .createdAt(visit.getCreatedAt() != null ? visit.getCreatedAt() : Instant.EPOCH)
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<DoctorVisit> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<DoctorVisit> findByProfileId(UUID profileId, LocalDate from, LocalDate to) {
            lastProfileId = profileId;
            lastFrom = from;
            lastTo = to;
            return store.values().stream()
                    .filter(v -> v.getProfileId().equals(profileId))
                    .filter(v -> from == null || !v.getFromDate().isBefore(from))
                    .filter(v -> to == null || !v.getFromDate().isAfter(to))
                    .toList();
        }

        @Override
        public List<DoctorVisit> findByProfileId(UUID profileId, LocalDate from, LocalDate to, int page, int size) {
            List<DoctorVisit> all = findByProfileId(profileId, from, to);
            int start = Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            return all.subList(start, end);
        }

        @Override
        public long countByProfileId(UUID profileId, LocalDate from, LocalDate to) {
            return findByProfileId(profileId, from, to).size();
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }
    }
}
