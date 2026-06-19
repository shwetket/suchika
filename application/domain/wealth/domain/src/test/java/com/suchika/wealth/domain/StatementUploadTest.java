package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatementUploadTest {

    @Test
    void builder_defaultStatus_isPending() {
        UUID accountId = UUID.randomUUID();

        StatementUpload upload = StatementUpload.builder()
                .accountId(accountId)
                .fileName("statement.csv")
                .uploadDate(Instant.parse("2026-06-01T00:00:00Z"))
                .build();

        assertEquals(UploadStatus.PENDING, upload.getStatus());
    }

    @Test
    void setStatus_mutatesStatusField() {
        StatementUpload upload = StatementUpload.builder()
                .accountId(UUID.randomUUID())
                .fileName("statement.csv")
                .uploadDate(Instant.parse("2026-06-01T00:00:00Z"))
                .build();

        assertEquals(UploadStatus.PENDING, upload.getStatus());

        upload.setStatus(UploadStatus.SUCCESS);
        assertEquals(UploadStatus.SUCCESS, upload.getStatus());

        upload.setStatus(UploadStatus.FAILED);
        assertEquals(UploadStatus.FAILED, upload.getStatus());
    }

    @Test
    void builder_allFields_roundTripsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant uploadDate = Instant.parse("2026-06-01T10:00:00Z");

        StatementUpload upload = StatementUpload.builder()
                .id(id)
                .accountId(accountId)
                .fileName("june_statement.pdf")
                .uploadDate(uploadDate)
                .status(UploadStatus.SUCCESS)
                .build();

        assertEquals(id, upload.getId());
        assertEquals(accountId, upload.getAccountId());
        assertEquals("june_statement.pdf", upload.getFileName());
        assertEquals(uploadDate, upload.getUploadDate());
        assertEquals(UploadStatus.SUCCESS, upload.getStatus());
    }
}
