package com.suchika.profile.ports.input;

import com.suchika.profile.domain.BloodType;
import com.suchika.profile.domain.Gender;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.domain.RelationToAdmin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProfileUseCase {

    Profile createProfile(UUID adminId, String fullName, LocalDate dob,
                          RelationToAdmin relationToAdmin, String emailAddress,
                          Gender gender, BloodType bloodType);

    Profile getProfile(UUID profileId);

    List<Profile> listProfiles(UUID adminId, Boolean isActive);

    Profile updateProfile(UUID profileId, String emailAddress, Gender gender,
                          BloodType bloodType, Boolean isActive);

    void deactivateProfile(UUID profileId);
}
