package com.suchika.profile.adapters.service;

import com.suchika.profile.domain.BloodType;
import com.suchika.profile.domain.Gender;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.domain.RelationToAdmin;
import com.suchika.profile.ports.input.ProfileUseCase;
import com.suchika.profile.ports.output.AdminRepository;
import com.suchika.profile.ports.output.ProfileRepository;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProfileService implements ProfileUseCase {

    private static final String PROFILE_NOT_FOUND = "Profile not found: ";

    @Inject
    ProfileRepository profileRepository;

    @Inject
    AdminRepository adminRepository;

    @Override
    @Transactional
    public Profile createProfile(UUID adminId, String fullName, LocalDate dob,
                                 RelationToAdmin relationToAdmin, String emailAddress,
                                 Gender gender, BloodType bloodType) {
        adminRepository.findById(adminId)
            .orElseThrow(() -> new NotFoundException("Admin not found: " + adminId));

        Profile profile = new Profile();
        profile.setAdminId(adminId);
        profile.setFullName(fullName);
        profile.setDob(dob);
        profile.setRelationToAdmin(relationToAdmin);
        profile.setEmailAddress(emailAddress);
        profile.setGender(gender);
        profile.setBloodType(bloodType);
        profile.setActive(true);

        Profile saved = profileRepository.save(profile);
        AppLogger.info("Profile created: %s for admin: %s", saved.getId(), adminId);
        return saved;
    }

    @Override
    public Profile getProfile(UUID profileId) {
        return profileRepository.findById(profileId)
            .orElseThrow(() -> new NotFoundException(PROFILE_NOT_FOUND + profileId));
    }

    @Override
    public List<Profile> listProfiles(UUID adminId, Boolean isActive) {
        return profileRepository.findAll(adminId, isActive);
    }

    @Override
    @Transactional
    public Profile updateProfile(UUID profileId, String emailAddress, Gender gender,
                                 BloodType bloodType, Boolean isActive) {
        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new NotFoundException(PROFILE_NOT_FOUND + profileId));

        if (emailAddress != null) profile.setEmailAddress(emailAddress);
        if (gender != null) profile.setGender(gender);
        if (bloodType != null) profile.setBloodType(bloodType);
        if (isActive != null) profile.setActive(isActive);

        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void deactivateProfile(UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new NotFoundException(PROFILE_NOT_FOUND + profileId));

        profile.setActive(false);
        profileRepository.save(profile);
        AppLogger.info("Profile deactivated: %s", profileId);
    }
}
