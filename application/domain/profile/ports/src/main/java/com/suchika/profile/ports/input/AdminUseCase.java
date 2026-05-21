package com.suchika.profile.ports.input;

import com.suchika.profile.domain.Admin;

import java.util.List;
import java.util.UUID;

public interface AdminUseCase {

    Admin createAdmin(String displayName, String emailAddress);

    Admin getAdmin(UUID adminId);

    List<Admin> listAdmins();

    Admin updateAdmin(UUID adminId, String displayName, String emailAddress, Boolean isActive);

    void deactivateAdmin(UUID adminId);
}
