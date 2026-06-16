package com.suchika.health.adapters.services;

import com.suchika.health.domain.DoctorVisit;
import com.suchika.health.ports.output.DoctorVisitRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DoctorVisitServiceTest {

    private StubDoctorVisitRepository repository;
    private DoctorVisitService service;

    @BeforeEach
    void setUp() {
        repository = new StubDoctorVisitRepository();
        service = new DoctorVisitService(repository);
    }

    @Test
    void create_visited_doctor_happy_path() {
        UUID profileId = UUID.randomUUID();
        DoctorVisit visit = service.create(profileId, LocalDate.now(), null,
                true, "Dr. Sharma", "Apollo", "General", null, null, null, null);

        assertNotNull(visit);
        assertEquals("Dr. Sharma", visit.getDoctorName());
        assertTrue(visit.isVisitedDoctor());
    }

    @Test
    void create_illness_without_visit() {
        UUID profileId = UUID.randomUUID();
        DoctorVisit visit = service.create(profileId, LocalDate.now(), null,
                false, null, null, null, "Fever", null, null, null);

        assertFalse(visit.isVisitedDoctor());
        assertNull(visit.getDoctorName());
    }

    @Test
    void create_requires_doctor_name_when_visited() {
        assertThrows(BadRequestException.class, () ->
                service.create(UUID.randomUUID(), LocalDate.now(), null,
                        true, null, null, null, null, null, null, null));
    }

    @Test
    void create_rejects_to_date_before_from_date() {
        assertThrows(BadRequestException.class, () ->
                service.create(UUID.randomUUID(), LocalDate.now(), LocalDate.now().minusDays(1),
                        false, null, null, null, null, null, null, null));
    }

    @Test
    void create_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () ->
                service.create(null, LocalDate.now(), null,
                        false, null, null, null, null, null, null, null));
    }

    @Test
    void create_rejects_null_from_date() {
        assertThrows(BadRequestException.class, () ->
                service.create(UUID.randomUUID(), null, null,
                        false, null, null, null, null, null, null, null));
    }

    @Test
    void update_partial_fields() {
        UUID profileId = UUID.randomUUID();
        DoctorVisit created = service.create(profileId, LocalDate.now(), null,
                true, "Dr. Sharma", null, null, null, null, null, null);

        DoctorVisit updated = service.update(created.getId(), null, null,
                "Apollo Hospital", null, null, "Viral fever", null, null);

        assertEquals("Dr. Sharma", updated.getDoctorName());
        assertEquals("Apollo Hospital", updated.getHospitalName());
        assertEquals("Viral fever", updated.getDiagnosis());
    }

    @Test
    void update_rejects_to_date_before_from_date() {
        DoctorVisit created = service.create(UUID.randomUUID(), LocalDate.now(), null,
                false, null, null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () ->
                service.update(created.getId(), LocalDate.now().minusDays(1),
                        null, null, null, null, null, null, null));
    }

    @Test
    void getById_throws_not_found() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void listByProfile_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () -> service.listByProfile(null));
    }

    @Test
    void delete_throws_not_found_for_unknown_id() {
        assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubDoctorVisitRepository implements DoctorVisitRepository {

        private final Map<UUID, DoctorVisit> store = new LinkedHashMap<>();

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
                    .createdAt(visit.getCreatedAt() != null ? visit.getCreatedAt() : Instant.now())
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<DoctorVisit> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<DoctorVisit> findByProfileId(UUID profileId) {
            return store.values().stream()
                    .filter(v -> v.getProfileId().equals(profileId))
                    .toList();
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
