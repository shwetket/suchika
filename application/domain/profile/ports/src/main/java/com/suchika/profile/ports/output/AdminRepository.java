package com.suchika.profile.ports.output;

import com.suchika.profile.domain.Admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository {

    Admin save(Admin admin);

    Optional<Admin> findById(UUID id);

    List<Admin> findAll();

    boolean existsByEmailAddress(String emailAddress);
}
