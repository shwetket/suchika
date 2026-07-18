package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogRepository;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Shared {@code save}/{@code findSince} Panache query logic for a domain's
 * own {@code error_log} table -- extracted 2026-07-13 from four
 * byte-for-byte-identical per-domain {@code ErrorLogPanacheRepository}
 * classes (Phase 4 Application Console, ADR-023).
 *
 * <p>Each domain still owns its own JPA {@code @Entity} class ({@code E})
 * and its own Panache DAO -- this base class never touches a concrete
 * entity type directly, only through the small {@link #dao()}/{@link
 * #newEntity}/{@link #toDomain} hooks the concrete per-domain subclass
 * implements. This keeps the actual {@code @Entity} class, and the four
 * separate {@code error_log} schemas/tables/Flyway migrations behind them,
 * unambiguously per-domain (ADR-003/ADR-006) -- only the query logic moved.
 *
 * @param <E> the domain's own Panache entity type
 */
public abstract class AbstractErrorLogPanacheRepository<E extends PanacheEntityBase> implements ErrorLogRepository {

    protected abstract PanacheRepositoryBase<E, UUID> dao();

    protected abstract E newEntity(String errorCode, int httpStatus, String message, String details);

    protected abstract ErrorLog toDomain(E entity);

    @Override
    @Transactional
    public void save(String errorCode, int httpStatus, String message, String details) {
        dao().persist(newEntity(errorCode, httpStatus, message, details));
    }

    @Override
    public List<ErrorLog> findSince(Instant since, int limit) {
        var query = since != null
                ? dao().find("createdAt >= ?1 order by createdAt desc", since)
                : dao().find("order by createdAt desc");
        return query.page(0, limit)
                .stream()
                .map(this::toDomain)
                .toList();
    }
}
