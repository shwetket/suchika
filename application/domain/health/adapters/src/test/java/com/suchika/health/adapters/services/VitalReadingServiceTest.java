package com.suchika.health.adapters.services;

import com.suchika.health.domain.VitalReading;
import com.suchika.health.domain.VitalType;
import com.suchika.health.ports.input.PagedVitalReadings;
import com.suchika.health.ports.input.UpdateVitalReadingCommand;
import com.suchika.health.ports.output.VitalReadingRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VitalReadingServiceTest {

    private StubVitalReadingRepository repository;
    private VitalReadingService service;

    @BeforeEach
    void setUp() {
        repository = new StubVitalReadingRepository();
        service = new VitalReadingService(repository);
    }

    @Test
    void record_weight_happy_path() {
        UUID profileId = UUID.randomUUID();
        VitalReading saved = service.recordReading(profileId, VitalType.WEIGHT,
                LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("72.5"), null, "kg", null);

        assertNotNull(saved);
        assertEquals(profileId, saved.getProfileId());
        assertEquals(VitalType.WEIGHT, saved.getVitalType());
        assertEquals(new BigDecimal("72.5"), saved.getValuePrimary());
    }

    @Test
    void record_blood_pressure_requires_secondary_value() {
        UUID profileId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () ->
                service.recordReading(profileId, VitalType.BLOOD_PRESSURE,
                        LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("120"), null, "mmHg", null));
    }

    @Test
    void record_blood_pressure_with_both_values_succeeds() {
        UUID profileId = UUID.randomUUID();
        VitalReading saved = service.recordReading(profileId, VitalType.BLOOD_PRESSURE,
                LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("120"), new BigDecimal("80"), "mmHg", null);

        assertEquals(new BigDecimal("120"), saved.getValuePrimary());
        assertEquals(new BigDecimal("80"), saved.getValueSecondary());
    }

    @Test
    void record_rejects_zero_value() {
        UUID profileId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () ->
                service.recordReading(profileId, VitalType.WEIGHT,
                        LocalDate.of(2024, Month.JANUARY, 15), BigDecimal.ZERO, null, "kg", null));
    }

    @Test
    void record_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () ->
                service.recordReading(null, VitalType.WEIGHT,
                        LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("70"), null, "kg", null));
    }

    @Test
    void record_rejects_blank_unit() {
        assertThrows(BadRequestException.class, () ->
                service.recordReading(UUID.randomUUID(), VitalType.WEIGHT,
                        LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("70"), null, "  ", null));
    }

    @Test
    void getById_throws_not_found_for_unknown_id() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.getById(unknownId));
    }

    @Test
    void listByProfile_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () -> service.listByProfile(null, null));
    }

    @Test
    void listByProfile_returns_readings_for_profile() {
        UUID profileId = UUID.randomUUID();
        service.recordReading(profileId, VitalType.WEIGHT, LocalDate.of(2024, Month.JANUARY, 14), new BigDecimal("72"), null, "kg", null);
        service.recordReading(profileId, VitalType.WEIGHT, LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("71.5"), null, "kg", null);

        List<VitalReading> readings = service.listByProfile(profileId, null);
        assertEquals(2, readings.size());
    }

    @Test
    void delete_throws_not_found_for_unknown_id() {
        UUID unknownId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.delete(unknownId));
    }

    @Test
    void listByProfilePaginated_rejects_null_profile_id() {
        assertThrows(BadRequestException.class, () -> service.listByProfilePaginated(null, null, 0, 10));
    }

    @Test
    void listByProfilePaginated_returnsRequestedPageAndTotalCount() {
        UUID profileId = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            service.recordReading(profileId, VitalType.WEIGHT, LocalDate.of(2024, Month.JANUARY, i),
                    new BigDecimal("70"), null, "kg", null);
        }

        PagedVitalReadings result = service.listByProfilePaginated(profileId, null, 1, 2);

        assertEquals(2, result.readings().size());
        assertEquals(5, result.totalCount());
    }

    @Test
    void listByProfilePaginated_filtersByVitalType() {
        UUID profileId = UUID.randomUUID();
        service.recordReading(profileId, VitalType.WEIGHT, LocalDate.of(2024, Month.JANUARY, 14), new BigDecimal("72"), null, "kg", null);
        service.recordReading(profileId, VitalType.HEART_RATE, LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("70"), null, "bpm", null);

        PagedVitalReadings result = service.listByProfilePaginated(profileId, VitalType.HEART_RATE, 0, 10);

        assertEquals(1, result.readings().size());
        assertEquals(1, result.totalCount());
        assertEquals(VitalType.HEART_RATE, result.readings().get(0).getVitalType());
    }

    @Test
    void update_partial_fields() {
        UUID profileId = UUID.randomUUID();
        VitalReading created = service.recordReading(profileId, VitalType.WEIGHT,
                LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("72.5"), null, "kg", "morning");

        UpdateVitalReadingCommand command = new UpdateVitalReadingCommand(
                null, new BigDecimal("71.0"), null, null, null);
        VitalReading updated = service.update(created.getId(), command);

        assertEquals(new BigDecimal("71.0"), updated.getValuePrimary());
        assertEquals("kg", updated.getUnit());
        assertEquals("morning", updated.getNotes());
        assertEquals(VitalType.WEIGHT, updated.getVitalType());
    }

    @Test
    void update_rejects_zero_value_primary() {
        UUID profileId = UUID.randomUUID();
        VitalReading created = service.recordReading(profileId, VitalType.WEIGHT,
                LocalDate.of(2024, Month.JANUARY, 15), new BigDecimal("72.5"), null, "kg", null);
        UUID createdId = created.getId();

        UpdateVitalReadingCommand command = new UpdateVitalReadingCommand(
                null, BigDecimal.ZERO, null, null, null);
        assertThrows(BadRequestException.class, () -> service.update(createdId, command));
    }

    @Test
    void update_throws_not_found_for_unknown_id() {
        UUID unknownId = UUID.randomUUID();
        UpdateVitalReadingCommand command = new UpdateVitalReadingCommand(
                null, new BigDecimal("70"), null, null, null);
        assertThrows(NotFoundException.class, () -> service.update(unknownId, command));
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubVitalReadingRepository implements VitalReadingRepository {

        private final java.util.Map<UUID, VitalReading> store = new java.util.LinkedHashMap<>();

        @Override
        public VitalReading save(VitalReading reading) {
            UUID id = reading.getId() != null ? reading.getId() : UUID.randomUUID();
            VitalReading stored = VitalReading.builder()
                    .id(id)
                    .profileId(reading.getProfileId())
                    .vitalType(reading.getVitalType())
                    .readingDate(reading.getReadingDate())
                    .valuePrimary(reading.getValuePrimary())
                    .valueSecondary(reading.getValueSecondary())
                    .unit(reading.getUnit())
                    .notes(reading.getNotes())
                    .createdAt(Instant.EPOCH)
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<VitalReading> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType) {
            return store.values().stream()
                    .filter(r -> r.getProfileId().equals(profileId))
                    .filter(r -> vitalType == null || r.getVitalType() == vitalType)
                    .toList();
        }

        @Override
        public List<VitalReading> findByProfileId(UUID profileId, VitalType vitalType, int page, int size) {
            List<VitalReading> all = findByProfileId(profileId, vitalType);
            int start = Math.min(page * size, all.size());
            int end = Math.min(start + size, all.size());
            return all.subList(start, end);
        }

        @Override
        public long countByProfileId(UUID profileId, VitalType vitalType) {
            return findByProfileId(profileId, vitalType).size();
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
