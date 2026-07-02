package com.suchika.profile.adapters.service;

import com.suchika.profile.domain.Admin;
import com.suchika.profile.ports.input.AdminUseCase;
import com.suchika.profile.ports.output.AdminRepository;
import com.suchika.profile.ports.output.ProfileRepository;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.ConflictException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AdminService implements AdminUseCase {

    private static final String ADMIN_NOT_FOUND = "Admin not found: ";

    private final AdminRepository adminRepository;
    private final ProfileRepository profileRepository;

    public AdminService(AdminRepository adminRepository, ProfileRepository profileRepository) {
        this.adminRepository = adminRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public Admin createAdmin(String displayName, String emailAddress) {
        if (emailAddress != null && adminRepository.existsByEmailAddress(emailAddress)) {
            throw new ConflictException("An admin with this email address already exists");
        }
        Admin admin = new Admin();
        admin.setDisplayName(displayName);
        admin.setEmailAddress(emailAddress);
        admin.setActive(true);
        Admin saved = adminRepository.save(admin);
        AppLogger.info("Admin created: %s", saved.getId());
        return saved;
    }

    @Override
    public Admin getAdmin(UUID adminId) {
        return adminRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException(ADMIN_NOT_FOUND + adminId));
    }

    @Override
    public List<Admin> listAdmins() {
        return adminRepository.findAll();
    }

    @Override
    @Transactional
    public Admin updateAdmin(UUID adminId, String displayName, String emailAddress, Boolean isActive) {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException(ADMIN_NOT_FOUND + adminId));

        if (displayName != null) admin.setDisplayName(displayName);
        if (emailAddress != null) {
            if (!emailAddress.equals(admin.getEmailAddress()) && adminRepository.existsByEmailAddress(emailAddress)) {
                throw new ConflictException("An admin with this email address already exists");
            }
            admin.setEmailAddress(emailAddress);
        }
        if (Boolean.FALSE.equals(isActive)) {
            long activeProfiles = profileRepository.countActiveByAdminId(adminId);
            if (activeProfiles > 0) {
                throw new ConflictException("Cannot deactivate admin with " + activeProfiles + " active profile(s)");
            }
        }
        if (isActive != null) admin.setActive(isActive);

        return adminRepository.save(admin);
    }

    @Override
    @Transactional
    public void deactivateAdmin(UUID adminId) {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException(ADMIN_NOT_FOUND + adminId));

        long activeProfiles = profileRepository.countActiveByAdminId(adminId);
        if (activeProfiles > 0) {
            throw new ConflictException("Cannot deactivate admin with " + activeProfiles + " active profile(s)");
        }

        admin.setActive(false);
        adminRepository.save(admin);
        AppLogger.info("Admin deactivated: %s", adminId);
    }

    @Override
    @Transactional
    public Admin updatePolicySettings(UUID adminId, Map<String, String> settings) {
        if (settings == null) {
            throw new BadRequestException("policy_settings must not be null");
        }
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException(ADMIN_NOT_FOUND + adminId));

        Map<String, String> merged = new HashMap<>(admin.getPolicySettings());
        settings.forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });
        admin.setPolicySettings(merged);

        Admin saved = adminRepository.save(admin);
        AppLogger.info("Admin policy settings updated: %s", adminId);
        return saved;
    }
}
