package com.suchika.household.adapters.service;

import com.suchika.household.domain.Goal;
import com.suchika.household.domain.GoalStatus;
import com.suchika.household.ports.output.GoalRepository;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoalServiceTest {

    private StubGoalRepository repository;
    private GoalService service;

    @BeforeEach
    void setUp() {
        repository = new StubGoalRepository();
        service = new GoalService(repository);
    }

    @Test
    void create_happyPath_returnsGoalWithId() {
        UUID profileId = UUID.randomUUID();
        Goal goal = service.create(profileId, "Emergency Fund", new BigDecimal("500000"),
                new BigDecimal("10000"), LocalDate.of(2027, Month.JANUARY, 1), "Critical savings");

        assertNotNull(goal.getId());
        assertEquals(profileId, goal.getProfileId());
        assertEquals("Emergency Fund", goal.getGoalName());
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(goal.getCurrentAmount()));
    }

    @Test
    void list_filterByStatus_returnsOnlyMatchingGoals() {
        UUID profileId = UUID.randomUUID();
        service.create(profileId, "Active Goal", new BigDecimal("100000"), null, null, null);
        service.update(
                service.create(profileId, "Paused Goal", new BigDecimal("50000"), null, null, null).getId(),
                null, null, null, null, GoalStatus.PAUSED, null);

        List<Goal> activeGoals = service.list(profileId, GoalStatus.ACTIVE);

        assertEquals(1, activeGoals.size());
        assertEquals("Active Goal", activeGoals.get(0).getGoalName());
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.get(UUID.randomUUID()));
    }

    @Test
    void get_found_returnsGoal() {
        UUID profileId = UUID.randomUUID();
        Goal created = service.create(profileId, "Car Fund", new BigDecimal("200000"),
                null, null, null);

        Goal found = service.get(created.getId());
        assertEquals(created.getId(), found.getId());
    }

    @Test
    void update_patchesGoalName() {
        UUID profileId = UUID.randomUUID();
        Goal created = service.create(profileId, "Old Name", new BigDecimal("100000"),
                null, null, null);

        Goal updated = service.update(created.getId(), "New Name", null, null, null, null, null);

        assertEquals("New Name", updated.getGoalName());
        assertEquals(0, new BigDecimal("100000").compareTo(updated.getTargetAmount()));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
                service.update(UUID.randomUUID(), "Name", null, null, null, null, null));
    }

    @Test
    void updateCurrentAmount_updatesField() {
        UUID profileId = UUID.randomUUID();
        Goal created = service.create(profileId, "Investment", new BigDecimal("1000000"),
                new BigDecimal("50000"), null, null);

        Goal updated = service.updateCurrentAmount(created.getId(), new BigDecimal("250000"));

        assertEquals(0, new BigDecimal("250000").compareTo(updated.getCurrentAmount()));
    }

    @Test
    void updateCurrentAmount_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
                service.updateCurrentAmount(UUID.randomUUID(), new BigDecimal("1000")));
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
    }

    @Test
    void delete_existing_removesGoal() {
        UUID profileId = UUID.randomUUID();
        Goal created = service.create(profileId, "To Delete", new BigDecimal("10000"),
                null, null, null);

        service.delete(created.getId());

        assertThrows(NotFoundException.class, () -> service.get(created.getId()));
    }

    // ── Stub repository ───────────────────────────────────────────────────────

    static class StubGoalRepository implements GoalRepository {

        private final Map<UUID, Goal> store = new LinkedHashMap<>();

        @Override
        public Goal save(Goal goal) {
            UUID id = goal.getId() != null ? goal.getId() : UUID.randomUUID();
            Goal stored = Goal.builder()
                    .id(id)
                    .profileId(goal.getProfileId())
                    .goalName(goal.getGoalName())
                    .targetAmount(goal.getTargetAmount())
                    .currentAmount(goal.getCurrentAmount())
                    .monthlySaving(goal.getMonthlySaving())
                    .targetDate(goal.getTargetDate())
                    .status(goal.getStatus())
                    .notes(goal.getNotes())
                    .createdAt(goal.getCreatedAt() != null ? goal.getCreatedAt() : Instant.EPOCH)
                    .build();
            store.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Goal> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Goal> findByProfileId(UUID profileId, GoalStatus status) {
            return store.values().stream()
                    .filter(g -> g.getProfileId().equals(profileId))
                    .filter(g -> status == null || g.getStatus() == status)
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
