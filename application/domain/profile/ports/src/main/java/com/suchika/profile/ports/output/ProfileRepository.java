package com.suchika.profile.ports.output;

import com.suchika.profile.domain.Profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findById(UUID id);

    List<Profile> findAll(UUID adminId, Boolean isActive);

    boolean existsById(UUID id);

    long countActiveByAdminId(UUID adminId);

    boolean existsSelfProfile(UUID adminId);
}
