package com.suchika.profile.adapters.persistence;

import com.suchika.profile.domain.Profile;
import com.suchika.profile.ports.output.ProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProfilePanacheRepository implements ProfileRepository {

    private final ProfileDao dao;

    public ProfilePanacheRepository(ProfileDao dao) {
        this.dao = dao;
    }

    @Override
    public Profile save(Profile profile) {
        ProfileEntity entity = ProfileEntity.from(profile);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Profile> findById(UUID id) {
        return dao.findByIdOptional(id).map(ProfileEntity::toDomain);
    }

    @Override
    public List<Profile> findAll(UUID adminId, Boolean isActive) {
        if (adminId != null && isActive != null) {
            return dao.find("adminId = ?1 and active = ?2", adminId, isActive)
                .stream().map(ProfileEntity::toDomain).toList();
        } else if (adminId != null) {
            return dao.find("adminId = ?1", adminId)
                .stream().map(ProfileEntity::toDomain).toList();
        } else if (isActive != null) {
            return dao.find("active = ?1", isActive)
                .stream().map(ProfileEntity::toDomain).toList();
        } else {
            return dao.listAll().stream().map(ProfileEntity::toDomain).toList();
        }
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.count("id = ?1", id) > 0;
    }

    @Override
    public long countActiveByAdminId(UUID adminId) {
        return dao.count("adminId = ?1 and active = true", adminId);
    }

    @Override
    public boolean existsSelfProfile(UUID adminId) {
        return dao.count("adminId = ?1 and relationToAdmin = ?2", adminId, "SELF") > 0;
    }
}
