package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Delegation coverage for {@link AbstractErrorLogService}, exercised through
 * a minimal concrete test subclass -- pure Java, no CDI context needed.
 * Replaces what were four near-identical per-domain {@code ErrorLogServiceTest}
 * classes (2026-07-13 ADR-023 revision); each domain's own {@code
 * ErrorLogServiceTest} is now a thin wiring smoke test only.
 */
class AbstractErrorLogServiceTest {

    private TestService service;
    private FakeErrorLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeErrorLogRepository();
        service = new TestService(repository);
    }

    @Test
    void record_delegatesToRepository() {
        service.recordError("NOT_FOUND", 404, "Profile not found", "ID: 123");

        assertEquals(1, repository.saved.size());
        assertEquals("NOT_FOUND", repository.saved.get(0).errorCode);
        assertEquals(404, repository.saved.get(0).httpStatus);
        assertEquals("Profile not found", repository.saved.get(0).message);
        assertEquals("ID: 123", repository.saved.get(0).details);
    }

    @Test
    void listErrors_delegatesToRepository() {
        Instant since = Instant.parse("2026-07-01T00:00:00Z");
        repository.toReturn = List.of(ErrorLog.builder().errorCode("BAD_REQUEST").httpStatus(400).message("bad").build());

        List<ErrorLog> result = service.listErrors(since, 25);

        assertEquals(1, result.size());
        assertEquals(since, repository.lastSince);
        assertEquals(25, repository.lastLimit);
    }

    static class TestService extends AbstractErrorLogService {
        private final ErrorLogRepository repository;

        TestService(ErrorLogRepository repository) {
            this.repository = repository;
        }

        @Override
        protected ErrorLogRepository repository() {
            return repository;
        }
    }

    static class SavedCall {
        final String errorCode;
        final int httpStatus;
        final String message;
        final String details;

        SavedCall(String errorCode, int httpStatus, String message, String details) {
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
            this.message = message;
            this.details = details;
        }
    }

    static class FakeErrorLogRepository implements ErrorLogRepository {
        final List<SavedCall> saved = new ArrayList<>();
        List<ErrorLog> toReturn = List.of();
        Instant lastSince;
        int lastLimit;

        @Override
        public void save(String errorCode, int httpStatus, String message, String details) {
            saved.add(new SavedCall(errorCode, httpStatus, message, details));
        }

        @Override
        public List<ErrorLog> findSince(Instant since, int limit) {
            lastSince = since;
            lastLimit = limit;
            return toReturn;
        }
    }
}
