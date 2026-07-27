package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.StatementUpload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Return type for {@link StatementUploadUseCase#uploadStatement}.
 * Wraps the persisted upload record together with per-row outcome data that
 * does not belong in the StatementUpload domain entity.
 */
public class UploadResult {

    private final StatementUpload upload;
    private final int insertedCount;
    private final List<SkippedRow> skippedDuplicates;

    public UploadResult(StatementUpload upload, int insertedCount, List<SkippedRow> skippedDuplicates) {
        this.upload = upload;
        this.insertedCount = insertedCount;
        this.skippedDuplicates = skippedDuplicates;
    }

    public StatementUpload getUpload() { return upload; }
    public int getInsertedCount() { return insertedCount; }
    public List<SkippedRow> getSkippedDuplicates() { return skippedDuplicates; }

    public record SkippedRow(LocalDate txnDate, BigDecimal amount, String description) {}
}
