package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.output.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransactionPanacheRepository implements TransactionRepository {

    private final TransactionDao dao;

    public TransactionPanacheRepository(TransactionDao dao) {
        this.dao = dao;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = TransactionEntity.from(transaction);
        if (entity.id == null) {
            dao.persist(entity);
        } else {
            entity = dao.getEntityManager().merge(entity);
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return dao.findByIdOptional(id).map(TransactionEntity::toDomain);
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId, LocalDate from, LocalDate to, TxnType txnType) {
        StringBuilder query = new StringBuilder("accountId = ?1");
        List<Object> params = new java.util.ArrayList<>();
        params.add(accountId);

        int paramIdx = 2;
        if (from != null) {
            query.append(" and txnDate >= ?").append(paramIdx++);
            params.add(from);
        }
        if (to != null) {
            query.append(" and txnDate <= ?").append(paramIdx++);
            params.add(to);
        }
        if (txnType != null) {
            query.append(" and txnType = ?").append(paramIdx);
            params.add(txnType.name());
        }
        query.append(" order by txnDate desc");

        return dao.find(query.toString(), params.toArray())
                .stream().map(TransactionEntity::toDomain).toList();
    }

    @Override
    public boolean existsByUniqueKey(UUID accountId, LocalDate txnDate, BigDecimal amount, TxnType txnType, String description) {
        return dao.count("accountId = ?1 and txnDate = ?2 and amount = ?3 and txnType = ?4 and description = ?5",
                accountId, txnDate, amount, txnType.name(), description) > 0;
    }
}
