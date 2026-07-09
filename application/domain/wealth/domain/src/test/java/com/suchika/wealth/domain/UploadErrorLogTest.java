package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UploadErrorLogTest {

    @Test
    void builder_allFields_gettersReturnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID uploadId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-01T10:00:00Z");
        List<String> missingColumns = List.of("date", "amount");

        UploadErrorLog log = UploadErrorLog.builder()
                .id(id)
                .uploadId(uploadId)
                .errorType("MISSING_REQUIRED_COLUMN")
                .missingColumns(missingColumns)
                .errorDetail("Columns 'date' and 'amount' not found in header row")
                .createdAt(createdAt)
                .build();

        assertEquals(id, log.getId());
        assertEquals(uploadId, log.getUploadId());
        assertEquals("MISSING_REQUIRED_COLUMN", log.getErrorType());
        assertEquals(missingColumns, log.getMissingColumns());
        assertEquals("Columns 'date' and 'amount' not found in header row", log.getErrorDetail());
        assertEquals(createdAt, log.getCreatedAt());
    }

    @Test
    void builder_noMissingColumns_missingColumnsIsNull() {
        UUID uploadId = UUID.randomUUID();

        UploadErrorLog log = UploadErrorLog.builder()
                .uploadId(uploadId)
                .errorType("PARSE_ERROR")
                .errorDetail("Unable to parse amount 'N/A' as a number")
                .build();

        assertNull(log.getId());
        assertNull(log.getMissingColumns());
        assertNull(log.getCreatedAt());
        assertEquals(uploadId, log.getUploadId());
        assertEquals("PARSE_ERROR", log.getErrorType());
        assertEquals("Unable to parse amount 'N/A' as a number", log.getErrorDetail());
    }
}
