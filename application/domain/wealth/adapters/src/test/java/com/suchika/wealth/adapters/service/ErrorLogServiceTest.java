package com.suchika.wealth.adapters.service;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Thin wiring smoke test only -- the full record/listErrors delegation
 * behavior is covered once, shared, by {@code AbstractErrorLogServiceTest}
 * in {@code shared-adapter} (2026-07-13 ADR-023 revision). This class just
 * confirms this domain's concrete {@link ErrorLogService} constructs and
 * binds its {@link ErrorLogRepository} correctly.
 */
class ErrorLogServiceTest {

    @Test
    void record_andListErrors_delegateToBoundRepository() {
        FakeErrorLogRepository repository = new FakeErrorLogRepository();
        ErrorLogService service = new ErrorLogService(repository);

        service.recordError("NOT_FOUND", 404, "Account not found", "ID: 123");
        List<ErrorLog> result = service.listErrors(null, 50);

        assertEquals(1, repository.saved.size());
        assertEquals(1, result.size());
    }

    static class FakeErrorLogRepository implements ErrorLogRepository {
        final List<String> saved = new ArrayList<>();

        @Override
        public void save(String errorCode, int httpStatus, String message, String details) {
            saved.add(errorCode);
        }

        @Override
        public List<ErrorLog> findSince(Instant since, int limit) {
            return List.of(ErrorLog.builder().errorCode("NOT_FOUND").httpStatus(404).message("x").build());
        }
    }
}
