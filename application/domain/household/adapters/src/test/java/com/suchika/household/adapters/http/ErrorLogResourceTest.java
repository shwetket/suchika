package com.suchika.household.adapters.http;

import com.suchika.shared.errorlog.ErrorLog;
import com.suchika.shared.errorlog.ErrorLogUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Thin wiring smoke test only -- the full since/limit-parsing and pagination
 * behavior is covered once, shared, by {@code AbstractErrorLogResourceTest}
 * in {@code shared-adapter} (2026-07-13 ADR-023 revision). This class just
 * confirms this domain's concrete {@link ErrorLogResource} constructs and
 * delegates to its bound {@link ErrorLogUseCase} correctly.
 */
class ErrorLogResourceTest {

    @Test
    void listErrors_delegatesToBoundUseCase() {
        StubUseCase useCase = new StubUseCase();
        useCase.toReturn = List.of(ErrorLog.builder()
                .errorCode("NOT_FOUND")
                .httpStatus(404)
                .message("Goal not found")
                .build());
        ErrorLogResource resource = new ErrorLogResource(useCase);

        var response = resource.listErrors(null, null);

        assertEquals(1, response.size());
        assertEquals("NOT_FOUND", response.get(0).errorCode);
    }

    static class StubUseCase implements ErrorLogUseCase {
        List<ErrorLog> toReturn = List.of();

        @Override
        public List<ErrorLog> listErrors(Instant since, int limit) {
            return toReturn;
        }
    }
}
