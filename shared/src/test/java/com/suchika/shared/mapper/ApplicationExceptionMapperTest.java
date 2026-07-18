package com.suchika.shared.mapper;

import com.suchika.shared.dto.ErrorResponse;
import com.suchika.shared.exception.ErrorLogRecorder;
import com.suchika.shared.exception.InternalServerException;
import com.suchika.shared.exception.NotFoundException;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Confirms {@link ApplicationExceptionMapper} maps HTTP status to response
 * body correctly AND logs at the right severity per the project's logging
 * convention: 4xx -> WARNING, 5xx -> ERROR (fixed 2026-07 — previously every
 * ApplicationException logged at WARN regardless of status).
 *
 * <p>Log level is verified by attaching a {@code java.util.logging.Handler}
 * to the root JUL logger. AppLogger routes through {@code io.quarkus.logging.Log},
 * which falls back to the JDK logging provider outside an augmented Quarkus
 * runtime (confirmed empirically) — every record from AppLogger.info/warn/error
 * carries the fixed logger name "com.suchika.shared.logging.AppLogger" because
 * that's the class Log resolves the caller from, not the call site's class.
 */
class ApplicationExceptionMapperTest {

    private static final String APP_LOGGER_CATEGORY = "com.suchika.shared.logging.AppLogger";

    private final ApplicationExceptionMapper mapper = new ApplicationExceptionMapper();
    private final List<LogRecord> captured = new CopyOnWriteArrayList<>();
    private Handler handler;

    @BeforeEach
    void attachHandler() {
        captured.clear();
        handler = new Handler() {
            @Override
            public void publish(LogRecord logRec) {
                captured.add(logRec);
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };
        Logger.getLogger("").addHandler(handler);
    }

    @AfterEach
    void detachHandler() {
        Logger.getLogger("").removeHandler(handler);
    }

    @Test
    void notFoundException_returns404_andLogsAtWarning() {
        Response response = mapper.toResponse(new NotFoundException("Transaction not found", "ID: 12345"));

        assertEquals(404, response.getStatus());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        assertEquals(404, body.getStatus());
        assertEquals("NOT_FOUND", body.getErrorCode());
        assertEquals("Transaction not found", body.getMessage());
        assertEquals("ID: 12345", body.getDetails());

        LogRecord logRec = onlyAppLoggerRecord();
        assertEquals(Level.WARNING, logRec.getLevel());
    }

    @Test
    void internalServerException_returns500_andLogsAtError() {
        Response response = mapper.toResponse(new InternalServerException("Failed to save transaction"));

        assertEquals(500, response.getStatus());
        ErrorResponse body = (ErrorResponse) response.getEntity();
        assertEquals(500, body.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", body.getErrorCode());

        LogRecord logRec = onlyAppLoggerRecord();
        assertEquals(Level.SEVERE, logRec.getLevel());
    }

    @Test
    void fourHundredLevelStatuses_neverLogAtError() {
        mapper.toResponse(new NotFoundException("not found"));

        assertFalse(captured.stream()
            .filter(r -> APP_LOGGER_CATEGORY.equals(r.getLoggerName()))
            .anyMatch(r -> r.getLevel() == Level.SEVERE));
    }

    @Test
    void notFoundException_noRecorderRegistered_stillReturns404() {
        // Plain `new ApplicationExceptionMapper()` (no CDI container) leaves the
        // Instance<ErrorLogRecorder> field null -- must not NPE, response is unchanged.
        Response response = new ApplicationExceptionMapper().toResponse(new NotFoundException("not found"));

        assertEquals(404, response.getStatus());
    }

    @Test
    void notFoundException_recorderRegistered_recordsErrorAndKeepsResponse() {
        FakeErrorLogRecorder recorder = new FakeErrorLogRecorder();
        ApplicationExceptionMapper testMapper = new ApplicationExceptionMapper(satisfiedInstance(recorder));

        Response response = testMapper.toResponse(new NotFoundException("Transaction not found", "ID: 12345"));

        assertEquals(404, response.getStatus());
        assertEquals(1, recorder.calls.size());
        assertEquals("NOT_FOUND", recorder.calls.get(0).errorCode);
        assertEquals(404, recorder.calls.get(0).httpStatus);
        assertEquals("Transaction not found", recorder.calls.get(0).message);
        assertEquals("ID: 12345", recorder.calls.get(0).details);
    }

    @Test
    void notFoundException_recorderThrows_responseStillReturned() {
        // A persistence failure while recording the error_log entry must never
        // change the HTTP response already built.
        Instance<ErrorLogRecorder> throwing = satisfiedInstance(new ErrorLogRecorder() {
            @Override
            public void recordError(String errorCode, int httpStatus, String message, String details) {
                throw new IllegalStateException("db unavailable");
            }
        });
        ApplicationExceptionMapper testMapper = new ApplicationExceptionMapper(throwing);

        Response response = testMapper.toResponse(new NotFoundException("not found"));

        assertEquals(404, response.getStatus());
    }

    @Test
    void notFoundException_unsatisfiedRecorderInstance_isSkipped() {
        ApplicationExceptionMapper testMapper = new ApplicationExceptionMapper(unsatisfiedInstance());

        Response response = testMapper.toResponse(new NotFoundException("not found"));

        assertEquals(404, response.getStatus());
    }

    private LogRecord onlyAppLoggerRecord() {
        List<LogRecord> appLoggerRecords = captured.stream()
            .filter(r -> APP_LOGGER_CATEGORY.equals(r.getLoggerName()))
            .toList();
        assertEquals(1, appLoggerRecords.size(), "expected exactly one AppLogger record");
        return appLoggerRecords.get(0);
    }

    private static Instance<ErrorLogRecorder> satisfiedInstance(ErrorLogRecorder recorder) {
        return new TestInstance(recorder);
    }

    private static Instance<ErrorLogRecorder> unsatisfiedInstance() {
        return new TestInstance(null);
    }

    private static class RecordedCall {
        final String errorCode;
        final int httpStatus;
        final String message;
        final String details;

        RecordedCall(String errorCode, int httpStatus, String message, String details) {
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
            this.message = message;
            this.details = details;
        }
    }

    private static class FakeErrorLogRecorder implements ErrorLogRecorder {
        final List<RecordedCall> calls = new ArrayList<>();

        @Override
        public void recordError(String errorCode, int httpStatus, String message, String details) {
            calls.add(new RecordedCall(errorCode, httpStatus, message, details));
        }
    }

    /**
     * Minimal hand-rolled {@code Instance<ErrorLogRecorder>} for plain-JUnit tests
     * (no CDI container available here) -- just enough of the contract for
     * {@link ApplicationExceptionMapper#recordError} to exercise both branches.
     */
    private static class TestInstance implements Instance<ErrorLogRecorder> {
        private final ErrorLogRecorder value;

        TestInstance(ErrorLogRecorder value) {
            this.value = value;
        }

        @Override
        public ErrorLogRecorder get() {
            if (value == null) {
                throw new NoSuchElementException("no ErrorLogRecorder registered");
            }
            return value;
        }

        @Override
        public boolean isUnsatisfied() {
            return value == null;
        }

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public Instance<ErrorLogRecorder> select(java.lang.annotation.Annotation... qualifiers) {
            return this;
        }

        @Override
        public <U extends ErrorLogRecorder> Instance<U> select(Class<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            throw new UnsupportedOperationException("not needed by this test");
        }

        @Override
        public <U extends ErrorLogRecorder> Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            throw new UnsupportedOperationException("not needed by this test");
        }

        @Override
        public void destroy(ErrorLogRecorder instance) {
            // no-op
        }

        @Override
        public jakarta.enterprise.inject.Instance.Handle<ErrorLogRecorder> getHandle() {
            throw new UnsupportedOperationException("not needed by this test");
        }

        @Override
        public Iterable<jakarta.enterprise.inject.Instance.Handle<ErrorLogRecorder>> handles() {
            throw new UnsupportedOperationException("not needed by this test");
        }

        @Override
        public Iterator<ErrorLogRecorder> iterator() {
            if (value == null) {
                return List.<ErrorLogRecorder>of().iterator();
            }
            return List.of(value).iterator();
        }
    }
}
