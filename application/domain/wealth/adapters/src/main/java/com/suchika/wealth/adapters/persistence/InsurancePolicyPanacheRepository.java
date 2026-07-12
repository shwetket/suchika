package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.ports.output.InsurancePolicyRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InsurancePolicyPanacheRepository implements InsurancePolicyRepository {

    private final InsurancePolicyDao dao;

    public InsurancePolicyPanacheRepository(InsurancePolicyDao dao) {
        this.dao = dao;
    }

    @Override
    public InsurancePolicy save(InsurancePolicy insurancePolicy) {
        InsurancePolicyEntity entity = InsurancePolicyEntity.from(insurancePolicy);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<InsurancePolicy> findById(UUID id, UUID adminId) {
        return dao.find("id = ?1 and adminId = ?2", id, adminId)
                .firstResultOptional()
                .map(InsurancePolicyEntity::toDomain);
    }

    @Override
    public List<InsurancePolicy> findAll(UUID adminId) {
        return dao.find("adminId = ?1 order by createdAt desc", adminId)
                .stream().map(InsurancePolicyEntity::toDomain).toList();
    }
}
