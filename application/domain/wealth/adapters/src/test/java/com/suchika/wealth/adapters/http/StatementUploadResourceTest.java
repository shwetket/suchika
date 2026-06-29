package com.suchika.wealth.adapters.http;

import com.suchika.wealth.adapters.http.dto.UploadErrorLogResponse;
import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.UploadErrorLog;
import com.suchika.wealth.domain.UploadStatus;
import com.suchika.wealth.ports.input.StatementUploadUseCase;
import com.suchika.wealth.ports.input.UploadResult;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatementUploadResourceTest {

    private StatementUploadResource resource;
    private StubUseCase useCase;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID UPLOAD_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new StubUseCase();
        resource = new StatementUploadResource(useCase);
    }

    @Test
    void getUploadErrors_returns200_withErrorList() {
        useCase.errorsToReturn = List.of(
                UploadErrorLog.builder()
                        .id(UUID.randomUUID())
                        .uploadId(UPLOAD_ID)
                        .errorType("MISSING_DATE_COLUMN")
                        .missingColumns(List.of("date"))
                        .errorDetail("Could not identify a date column.")
                        .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build()
        );

        Response response = resource.getUploadErrors(ACCOUNT_ID, UPLOAD_ID);

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        List<UploadErrorLogResponse> body = (List<UploadErrorLogResponse>) response.getEntity();
        assertEquals(1, body.size());
        assertEquals("MISSING_DATE_COLUMN", body.get(0).errorType);
        assertTrue(body.get(0).missingColumns.contains("date"));
        assertEquals("Could not identify a date column.", body.get(0).errorDetail);
    }

    @Test
    void getUploadErrors_returns200_emptyListWhenNoErrors() {
        useCase.errorsToReturn = List.of();

        Response response = resource.getUploadErrors(ACCOUNT_ID, UPLOAD_ID);

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        List<UploadErrorLogResponse> body = (List<UploadErrorLogResponse>) response.getEntity();
        assertTrue(body.isEmpty());
    }

    static class StubUseCase implements StatementUploadUseCase {
        List<UploadErrorLog> errorsToReturn = List.of();

        @Override
        public UploadResult uploadStatement(UUID accountId, String fileName, String csvContent) {
            StatementUpload upload = StatementUpload.builder()
                    .id(UUID.randomUUID())
                    .accountId(accountId)
                    .fileName(fileName)
                    .status(UploadStatus.SUCCESS)
                    .build();
            return new UploadResult(upload, 0, List.of());
        }

        @Override
        public void rollbackUpload(UUID uploadId) {
            throw new UnsupportedOperationException("not exercised by these tests");
        }

        @Override
        public StatementUpload getUpload(UUID uploadId) {
            return StatementUpload.builder().id(uploadId).status(UploadStatus.SUCCESS).build();
        }

        @Override
        public List<StatementUpload> listUploads(UUID accountId) {
            return List.of();
        }

        @Override
        public List<UploadErrorLog> getUploadErrors(UUID uploadId) {
            return errorsToReturn;
        }
    }
}
