package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.UploadErrorLog;
import com.suchika.wealth.domain.UploadStatus;
import com.suchika.wealth.ports.output.UploadErrorLogRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.domain.StatementUpload;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UploadErrorLogPanacheRepository.
 * Requires a running local PostgreSQL (app_db) with wealth seed applied.
 * Each test creates its own statement_upload row (FK anchor) within the test transaction.
 */
@QuarkusTest
@TestTransaction
class UploadErrorLogPanacheRepositoryTest {

    @Inject
    UploadErrorLogRepository errorLogRepo;

    @Inject
    StatementUploadRepository uploadRepo;

    private static final UUID SEEDED_ACCOUNT_ID = UUID.fromString("f3b90000-0000-0000-0000-000000000000");

    @Test
    void save_andFindByUploadId_roundTrip() {
        UUID uploadId = createUpload();

        errorLogRepo.save(uploadId, "MISSING_DATE_COLUMN", List.of("date"), "Could not find date column.");

        List<UploadErrorLog> results = errorLogRepo.findByUploadId(uploadId);

        assertEquals(1, results.size());
        UploadErrorLog log = results.get(0);
        assertNotNull(log.getId());
        assertEquals(uploadId, log.getUploadId());
        assertEquals("MISSING_DATE_COLUMN", log.getErrorType());
        assertTrue(log.getMissingColumns().contains("date"));
        assertEquals("Could not find date column.", log.getErrorDetail());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void findByUploadId_unknownId_returnsEmpty() {
        List<UploadErrorLog> results = errorLogRepo.findByUploadId(UUID.randomUUID());

        assertTrue(results.isEmpty());
    }

    @Test
    void findByUploadId_onlyReturnsLogsForThatUpload() {
        UUID uploadA = createUpload();
        UUID uploadB = createUpload();

        errorLogRepo.save(uploadA, "MISSING_DATE_COLUMN", List.of("date"), "Upload A error");
        errorLogRepo.save(uploadB, "MISSING_AMOUNT_COLUMN", List.of("amount"), "Upload B error");

        List<UploadErrorLog> resultsA = errorLogRepo.findByUploadId(uploadA);
        List<UploadErrorLog> resultsB = errorLogRepo.findByUploadId(uploadB);

        assertEquals(1, resultsA.size());
        assertEquals("MISSING_DATE_COLUMN", resultsA.get(0).getErrorType());

        assertEquals(1, resultsB.size());
        assertEquals("MISSING_AMOUNT_COLUMN", resultsB.get(0).getErrorType());
    }

    private UUID createUpload() {
        StatementUpload upload = uploadRepo.save(StatementUpload.builder()
                .accountId(SEEDED_ACCOUNT_ID)
                .fileName("test.csv")
                .status(UploadStatus.FAILED)
                .build());
        return upload.getId();
    }
}
