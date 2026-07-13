package com.suchika.profile.adapters.service;

import com.suchika.profile.domain.ErrorLog;
import com.suchika.profile.ports.output.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorLogServiceTest {

    private ErrorLogService service;
    private FakeErrorLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeErrorLogRepository();
        service = new ErrorLogService(repository);
    }

    @Test
    void record_delegatesToRepository() {
        service.record("NOT_FOUND", 404, "Profile not found", "ID: 123");

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
