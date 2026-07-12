package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.InsurancePolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsurancePolicyRepository {

    InsurancePolicy save(InsurancePolicy insurancePolicy);

    Optional<InsurancePolicy> findById(UUID id, UUID adminId);

    List<InsurancePolicy> findAll(UUID adminId);
}
