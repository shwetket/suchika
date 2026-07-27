package com.suchika.profile.adapters.service;

import com.suchika.profile.domain.Admin;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.ports.output.AdminRepository;
import com.suchika.profile.ports.output.ProfileRepository;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    private AdminService service;
    private FakeAdminRepository adminRepo;
    private FakeProfileRepository profileRepo;

    @BeforeEach
    void setUp() {
        adminRepo = new FakeAdminRepository();
        profileRepo = new FakeProfileRepository();
        service = new AdminService(adminRepo, profileRepo);
    }

    @Test
    void createAdmin_happyPath_returnsAdminWithId() {
        Admin result = service.createAdmin("Ketan", "ketan@example.com");

        assertNotNull(result.getId());
        assertEquals("Ketan", result.getDisplayName());
        assertEquals("ketan@example.com", result.getEmailAddress());
        assertTrue(result.isActive());
    }

    @Test
    void createAdmin_nullEmail_succeeds() {
        Admin result = service.createAdmin("Ketan", null);

        assertNotNull(result.getId());
        assertNull(result.getEmailAddress());
    }

    @Test
    void createAdmin_duplicateEmail_throwsConflict() {
        service.createAdmin("Ketan", "ketan@example.com");

        assertThrows(ConflictException.class,
            () -> service.createAdmin("Other", "ketan@example.com"));
    }

    @Test
    void getAdmin_found_returnsAdmin() {
        Admin created = service.createAdmin("Ketan", null);

        Admin found = service.getAdmin(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Ketan", found.getDisplayName());
    }

    @Test
    void getAdmin_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.getAdmin(UUID.randomUUID()));
    }

    @Test
    void listAdmins_returnsAllAdmins() {
        service.createAdmin("Ketan", null);
        service.createAdmin("Shweta", null);

        assertEquals(2, service.listAdmins().size());
    }

    @Test
    void updateAdmin_setIsActiveFalse_noActiveProfiles_succeeds() {
        Admin admin = service.createAdmin("Ketan", null);

        Admin result = service.updateAdmin(admin.getId(), null, null, false);

        assertFalse(result.isActive());
    }

    @Test
    void updateAdmin_setIsActiveFalse_withActiveProfiles_throwsConflict() {
        Admin admin = service.createAdmin("Ketan", null);
        profileRepo.addActiveProfile(admin.getId());

        assertThrows(ConflictException.class,
            () -> service.updateAdmin(admin.getId(), null, null, false));
    }

    @Test
    void updateAdmin_setIsActiveTrue_withActiveProfiles_skipsProfileCheck() {
        Admin admin = service.createAdmin("Ketan", null);
        profileRepo.addActiveProfile(admin.getId());

        assertDoesNotThrow(() -> service.updateAdmin(admin.getId(), null, null, true));
    }

    @Test
    void updateAdmin_isActiveNull_skipsProfileCheck() {
        Admin admin = service.createAdmin("Ketan", null);
        profileRepo.addActiveProfile(admin.getId());

        Admin result = service.updateAdmin(admin.getId(), "Ketan Updated", null, null);

        assertEquals("Ketan Updated", result.getDisplayName());
        assertTrue(result.isActive());
    }

    @Test
    void updateAdmin_emailChange_duplicateEmail_throwsConflict() {
        service.createAdmin("Ketan", "ketan@example.com");
        Admin other = service.createAdmin("Shweta", "shweta@example.com");

        assertThrows(ConflictException.class,
            () -> service.updateAdmin(other.getId(), null, "ketan@example.com", null));
    }

    @Test
    void deactivateAdmin_noActiveProfiles_setsInactive() {
        Admin admin = service.createAdmin("Ketan", null);

        service.deactivateAdmin(admin.getId());

        assertFalse(service.getAdmin(admin.getId()).isActive());
    }

    @Test
    void deactivateAdmin_withActiveProfiles_throwsConflict() {
        Admin admin = service.createAdmin("Ketan", null);
        profileRepo.addActiveProfile(admin.getId());

        assertThrows(ConflictException.class, () -> service.deactivateAdmin(admin.getId()));
    }

    @Test
    void deactivateAdmin_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> service.deactivateAdmin(UUID.randomUUID()));
    }

    @Test
    void updatePolicySettings_mergesIntoExisting() {
        Admin admin = service.createAdmin("Ketan", null);
        UUID adminId = admin.getId();

        Map<String, String> first = new HashMap<>();
        first.put("monthly_budget_cap", "50000");
        first.put("freedom_runway_months", "12");
        service.updatePolicySettings(adminId, first);

        Map<String, String> second = new HashMap<>();
        second.put("insurance_multiple", "10");
        Admin result = service.updatePolicySettings(adminId, second);

        assertEquals("50000", result.getPolicySettings().get("monthly_budget_cap"));
        assertEquals("12", result.getPolicySettings().get("freedom_runway_months"));
        assertEquals("10", result.getPolicySettings().get("insurance_multiple"));
    }

    @Test
    void updatePolicySettings_nullValue_skipsKey() {
        Admin admin = service.createAdmin("Ketan", null);
        UUID adminId = admin.getId();

        Map<String, String> initial = new HashMap<>();
        initial.put("monthly_budget_cap", "50000");
        service.updatePolicySettings(adminId, initial);

        Map<String, String> patch = new HashMap<>();
        patch.put("monthly_budget_cap", null);
        patch.put("freedom_runway_months", "6");
        Admin result = service.updatePolicySettings(adminId, patch);

        assertEquals("50000", result.getPolicySettings().get("monthly_budget_cap"),
            "null value must not overwrite existing key");
        assertEquals("6", result.getPolicySettings().get("freedom_runway_months"));
    }

    // ---- Fake repositories ----

    static class FakeAdminRepository implements AdminRepository {
        private final Map<UUID, Admin> store = new HashMap<>();

        @Override
        public Admin save(Admin admin) {
            if (admin.getId() == null) admin.setId(UUID.randomUUID());
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
            return store.values().stream()
                .anyMatch(a -> email != null && email.equals(a.getEmailAddress()));
        }
    }

    static class FakeProfileRepository implements ProfileRepository {
        private final Map<UUID, Profile> store = new HashMap<>();

        void addActiveProfile(UUID adminId) {
            Profile p = new Profile();
            p.setId(UUID.randomUUID());
            p.setAdminId(adminId);
            p.setActive(true);
            store.put(p.getId(), p);
        }

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
            return false;
        }
    }
}
