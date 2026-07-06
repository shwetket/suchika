package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.output.TransactionRepository;
import io.quarkus.panache.common.Page;
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
    public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
        Filter filter = buildFilter(accountId, profileId, from, to, txnType);
        return dao.find(filter.query(), filter.params().toArray())
                .stream().map(TransactionEntity::toDomain).toList();
    }

    @Override
    public List<Transaction> findByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType,
                                              int page, int size) {
        Filter filter = buildFilter(accountId, profileId, from, to, txnType);
        return dao.find(filter.query(), filter.params().toArray())
                .page(Page.of(page, size))
                .list()
                .stream().map(TransactionEntity::toDomain).toList();
    }

    @Override
    public long countByAccountId(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
        Filter filter = buildFilter(accountId, profileId, from, to, txnType);
        return dao.find(filter.query(), filter.params().toArray()).count();
    }

    /**
     * Shared predicate builder for {@code findByAccountId} (both variants) and
     * {@code countByAccountId} — keeps the filter logic in exactly one place
     * (Sonar CPD) now that there are three call sites needing the same predicate.
     */
    private Filter buildFilter(UUID accountId, UUID profileId, LocalDate from, LocalDate to, TxnType txnType) {
        StringBuilder query = new StringBuilder("accountId = ?1");
        List<Object> params = new java.util.ArrayList<>();
        params.add(accountId);

        int paramIdx = 2;
        if (profileId != null) {
            query.append(" and accountId in (select a.id from AccountEntity a where a.profileId = ?")
                    .append(paramIdx++).append(")");
            params.add(profileId);
        }
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

        return new Filter(query.toString(), params);
    }

    private record Filter(String query, List<Object> params) {
    }

    @Override
    public boolean existsByDeduplicationKey(UUID accountId, UUID profileId, LocalDate txnDate, BigDecimal amount, TxnType txnType) {
        if (profileId != null) {
            return dao.count("accountId = ?1 and txnDate = ?2 and amount = ?3 and txnType = ?4"
                            + " and accountId in (select a.id from AccountEntity a where a.profileId = ?5)",
                    accountId, txnDate, amount, txnType.name(), profileId) > 0;
        }
        return dao.count("accountId = ?1 and txnDate = ?2 and amount = ?3 and txnType = ?4",
                accountId, txnDate, amount, txnType.name()) > 0;
    }

    @Override
    public BigDecimal sumAmountByTxnType(UUID accountId, UUID profileId, TxnType txnType) {
        StringBuilder jpql = new StringBuilder(
                "select coalesce(sum(t.amount), 0) from TransactionEntity t "
                        + "where t.accountId = ?1 and t.txnType = ?2");
        List<Object> params = new java.util.ArrayList<>();
        params.add(accountId);
        params.add(txnType.name());

        if (profileId != null) {
            jpql.append(" and t.accountId in (select a.id from AccountEntity a where a.profileId = ?3)");
            params.add(profileId);
        }

        jakarta.persistence.TypedQuery<BigDecimal> query =
                dao.getEntityManager().createQuery(jpql.toString(), BigDecimal.class);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        return query.getSingleResult();
    }

    @Override
    public void deleteByUploadId(UUID uploadId) {
        dao.delete("uploadId", uploadId);
    }
}
