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
    public List<Account> findAll(AccountType accountType, Boolean isActive) {
        if (accountType != null && isActive != null) {
            return dao.find("accountType = ?1 and active = ?2", accountType.name(), isActive)
                    .stream().map(AccountEntity::toDomain).toList();
        }
        if (accountType != null) {
            return dao.find("accountType = ?1", accountType.name())
                    .stream().map(AccountEntity::toDomain).toList();
        }
        if (isActive != null) {
            return dao.find("active = ?1", isActive)
                    .stream().map(AccountEntity::toDomain).toList();
        }
        return dao.listAll().stream().map(AccountEntity::toDomain).toList();
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
