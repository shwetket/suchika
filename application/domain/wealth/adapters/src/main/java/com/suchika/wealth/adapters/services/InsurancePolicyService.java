package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.domain.InsurancePolicy;
import com.suchika.wealth.ports.input.CreateInsurancePolicyCommand;
import com.suchika.wealth.ports.input.InsurancePolicyUseCase;
import com.suchika.wealth.ports.input.UpdateInsurancePolicyCommand;
import com.suchika.wealth.ports.output.InsurancePolicyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class InsurancePolicyService implements InsurancePolicyUseCase {

    private static final String INSURANCE_POLICY_NOT_FOUND = "Insurance policy not found: ";

    private final InsurancePolicyRepository repository;

    public InsurancePolicyService(InsurancePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InsurancePolicy createInsurancePolicy(UUID adminId, CreateInsurancePolicyCommand command) {
        InsurancePolicy policy = InsurancePolicy.create(
                adminId,
                command.policyName(),
                command.provider(),
                command.policyType(),
                command.premiumAmount(),
                command.premiumFrequency(),
                command.coverageAmount());

        InsurancePolicy saved = repository.save(policy);
        AppLogger.info("Insurance policy created: %s (%s)", saved.getId(), command.policyType());
        return saved;
    }

    @Override
    public InsurancePolicy getInsurancePolicy(UUID id, UUID adminId) {
        return findOrThrow(id, adminId);
    }

    @Override
    public List<InsurancePolicy> listInsurancePolicies(UUID adminId) {
        return repository.findAll(adminId);
    }

    @Override
    @Transactional
    public InsurancePolicy updateInsurancePolicy(UUID id, UUID adminId, UpdateInsurancePolicyCommand command) {
        InsurancePolicy policy = findOrThrow(id, adminId);

        String policyName = command.policyName();
        if (policyName != null) {
            if (policyName.isBlank()) throw new BadRequestException("policy_name must not be blank");
            policy.setPolicyName(policyName);
        }
        String provider = command.provider();
        if (provider != null) {
            if (provider.isBlank()) throw new BadRequestException("provider must not be blank");
            policy.setProvider(provider);
        }
        if (command.premiumAmount() != null) {
            if (command.premiumAmount().signum() < 0) {
                throw new BadRequestException("premium_amount must not be negative");
            }
            policy.setPremiumAmount(command.premiumAmount());
        }
        if (command.premiumFrequency() != null) {
            policy.setPremiumFrequency(command.premiumFrequency());
        }
        if (command.coverageAmount() != null) {
            if (command.coverageAmount().signum() < 0) {
                throw new BadRequestException("coverage_amount must not be negative");
            }
            policy.setCoverageAmount(command.coverageAmount());
        }
        if (command.payoutStructure() != null) {
            Map<String, String> merged = new HashMap<>(policy.getPayoutStructure());
            merged.putAll(command.payoutStructure());
            policy.setPayoutStructure(merged);
        }
        if (command.isActive() != null) {
            policy.setActive(command.isActive());
        }

        return repository.save(policy);
    }

    @Override
    @Transactional
    public void deactivateInsurancePolicy(UUID id, UUID adminId) {
        InsurancePolicy policy = findOrThrow(id, adminId);
        policy.setActive(false);
        repository.save(policy);
        AppLogger.info("Insurance policy deactivated: %s", id);
    }

    private InsurancePolicy findOrThrow(UUID id, UUID adminId) {
        return repository.findById(id, adminId)
                .orElseThrow(() -> new NotFoundException(INSURANCE_POLICY_NOT_FOUND + id));
    }
}
