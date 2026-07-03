package com.suchika.profile.adapters.service;

import com.suchika.profile.domain.*;
import com.suchika.profile.ports.output.AdminRepository;
import com.suchika.profile.ports.output.ProfileRepository;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {

    private ProfileService service;
    private FakeAdminRepository adminRepo;
    private FakeProfileRepository profileRepo;

    @BeforeEach
    void setUp() {
        adminRepo = new FakeAdminRepository();
        profileRepo = new FakeProfileRepository();
        service = new ProfileService(profileRepo, adminRepo);
    }

    private UUID seedAdmin() {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        admin.setDisplayName("Ketan");
        admin.setActive(true);
        adminRepo.save(admin);
        return admin.getId();
    }

    @Test
    void createProfile_adminNotFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
            service.createProfile(UUID.randomUUID(), "Ketan", LocalDate.of(1988, Month.MARCH, 15),
                RelationToAdmin.SELF, null, null, null));
    }

    @Test
    void createProfile_happyPath_returnsProfileWithAllFields() {
        UUID adminId = seedAdmin();

        Profile result = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, "k@example.com", Gender.MALE, BloodType.A_POSITIVE);

        assertNotNull(result.getId());
        assertEquals(adminId, result.getAdminId());
        assertEquals("Ketan", result.getFullName());
        assertEquals(LocalDate.of(1988, Month.MARCH, 15), result.getDob());
        assertEquals(RelationToAdmin.SELF, result.getRelationToAdmin());
        assertEquals("k@example.com", result.getEmailAddress());
        assertEquals(Gender.MALE, result.getGender());
        assertEquals(BloodType.A_POSITIVE, result.getBloodType());
        assertTrue(result.isActive());
    }

    @Test
    void createProfile_nullOptionalFields_succeeds() {
        UUID adminId = seedAdmin();

        Profile result = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);

        assertNotNull(result.getId());
        assertNull(result.getEmailAddress());
        assertNull(result.getGender());
        assertNull(result.getBloodType());
    }

    @Test
    void createProfile_secondSelfProfileForSameAdmin_throwsConflictException() {
        UUID adminId = seedAdmin();
        service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);

        assertThrows(ConflictException.class, () ->
            service.createProfile(adminId, "Ketan Duplicate", LocalDate.of(1990, Month.JANUARY, 1),
                RelationToAdmin.SELF, null, null, null));
    }

    @Test
    void createProfile_secondNonSelfProfileForSameAdmin_succeeds() {
        UUID adminId = seedAdmin();
        service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);

        Profile spouse = service.createProfile(adminId, "Shweta", LocalDate.of(1990, Month.JANUARY, 1),
            RelationToAdmin.SPOUSE, null, null, null);

        assertNotNull(spouse.getId());
    }

    @Test
    void getProfile_found_returnsProfile() {
        UUID adminId = seedAdmin();
        Profile created = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);

        Profile found = service.getProfile(created.getId());

        assertEquals(created.getId(), found.getId());
    }

    @Test
    void getProfile_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.getProfile(UUID.randomUUID()));
    }

    @Test
    void listProfiles_filterByAdminId_returnsOnlyThatAdminsProfiles() {
        UUID adminId1 = seedAdmin();
        UUID adminId2 = seedAdmin();
        service.createProfile(adminId1, "Ketan", LocalDate.of(1988, Month.MARCH, 15), RelationToAdmin.SELF, null, null, null);
        service.createProfile(adminId2, "Shweta", LocalDate.of(1990, Month.JUNE, 20), RelationToAdmin.SELF, null, null, null);

        List<Profile> result = service.listProfiles(adminId1, null);

        assertEquals(1, result.size());
        assertEquals("Ketan", result.get(0).getFullName());
    }

    @Test
    void updateProfile_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
            () -> service.updateProfile(UUID.randomUUID(), "new@example.com", null, null, null));
    }

    @Test
    void updateProfile_patchesOnlyProvidedFields() {
        UUID adminId = seedAdmin();
        Profile created = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, "old@example.com", Gender.MALE, null);

        Profile updated = service.updateProfile(
            created.getId(), "new@example.com", null, BloodType.B_POSITIVE, null);

        assertEquals("new@example.com", updated.getEmailAddress());
        assertEquals(Gender.MALE, updated.getGender());       // not changed
        assertEquals(BloodType.B_POSITIVE, updated.getBloodType()); // set
        assertTrue(updated.isActive());                       // not changed
    }

    @Test
    void updateProfile_setIsActiveFalse_deactivatesProfile() {
        UUID adminId = seedAdmin();
        Profile created = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);

        Profile updated = service.updateProfile(created.getId(), null, null, null, false);

        assertFalse(updated.isActive());
    }

    @Test
    void deactivateProfile_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.deactivateProfile(UUID.randomUUID()));
    }

    @Test
    void deactivateProfile_setsActiveToFalse() {
        UUID adminId = seedAdmin();
        Profile created = service.createProfile(adminId, "Ketan", LocalDate.of(1988, Month.MARCH, 15),
            RelationToAdmin.SELF, null, null, null);
        assertTrue(created.isActive());

        service.deactivateProfile(created.getId());

        assertFalse(service.getProfile(created.getId()).isActive());
    }

    // ---- Fake repositories ----

    static class FakeAdminRepository implements AdminRepository {
        private final Map<UUID, Admin> store = new HashMap<>();

        @Override
        public Admin save(Admin admin) {
            store.put(admin.getId(), admin);
            return admin;
        }

        @Override
        public Optional<Admin> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Admin> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public boolean existsByEmailAddress(String email) {
            return false;
        }
    }

    static class FakeProfileRepository implements ProfileRepository {
        private final Map<UUID, Profile> store = new HashMap<>();

        @Override
        public Profile save(Profile profile) {
            if (profile.getId() == null) profile.setId(UUID.randomUUID());
            store.put(profile.getId(), profile);
            return profile;
        }

        @Override
        public Optional<Profile> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Profile> findAll(UUID adminId, Boolean isActive) {
            return store.values().stream()
                .filter(p -> adminId == null || adminId.equals(p.getAdminId()))
                .filter(p -> isActive == null || isActive.equals(p.isActive()))
                .toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }

        @Override
        public long countActiveByAdminId(UUID adminId) {
            return store.values().stream()
                .filter(p -> adminId.equals(p.getAdminId()) && p.isActive())
                .count();
        }

        @Override
        public boolean existsSelfProfile(UUID adminId) {
            return store.values().stream()
                .anyMatch(p -> adminId.equals(p.getAdminId()) && p.getRelationToAdmin() == RelationToAdmin.SELF);
        }
    }
}
