package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.Account;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.output.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountPanacheRepository implements AccountRepository {

    private final AccountDao dao;

    public AccountPanacheRepository(AccountDao dao) {
        this.dao = dao;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountEntity.from(account);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return dao.findByIdOptional(id).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findById(UUID id, UUID profileId) {
        return dao.find("id = ?1 and profileId = ?2", id, profileId)
                .firstResultOptional()
                .map(AccountEntity::toDomain);
    }

    @Override
    public List<Account> findAll(UUID profileId, AccountType accountType, Boolean isActive) {
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();

        if (profileId != null) {
            query.append("profileId = ?").append(params.size() + 1);
            params.add(profileId);
        }
        if (accountType != null) {
            if (!query.isEmpty()) query.append(" and ");
            query.append("accountType = ?").append(params.size() + 1);
            params.add(accountType.name());
        }
        if (isActive != null) {
            if (!query.isEmpty()) query.append(" and ");
            query.append("active = ?").append(params.size() + 1);
            params.add(isActive);
        }

        if (query.isEmpty()) {
            return dao.listAll().stream().map(AccountEntity::toDomain).toList();
        }
        return dao.find(query.toString(), params.toArray())
                .stream().map(AccountEntity::toDomain).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return dao.findByIdOptional(id).isPresent();
    }

    @Override
    public boolean hasTransactions(UUID accountId) {
        Long count = (Long) dao.getEntityManager()
                .createNativeQuery("SELECT COUNT(*) FROM wealth.transaction WHERE account_id = ?1")
                .setParameter(1, accountId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
