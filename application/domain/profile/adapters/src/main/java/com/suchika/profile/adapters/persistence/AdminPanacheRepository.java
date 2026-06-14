package com.suchika.profile.adapters.persistence;

import com.suchika.profile.domain.Admin;
import com.suchika.profile.ports.output.AdminRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminPanacheRepository implements AdminRepository {

    @Inject
    AdminDao dao;

    @Override
    public Admin save(Admin admin) {
        AdminEntity entity = AdminEntity.from(admin);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Admin> findById(UUID id) {
        return dao.findByIdOptional(id).map(AdminEntity::toDomain);
    }

    @Override
    public List<Admin> findAll() {
        return dao.listAll().stream().map(AdminEntity::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmailAddress(String emailAddress) {
        return dao.count("emailAddress = ?1", emailAddress) > 0;
    }
}
