package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.adapters.services.StatementCsvParser.ParsedRow;
import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.UploadErrorLog;
import com.suchika.wealth.domain.UploadStatus;
import com.suchika.wealth.ports.input.StatementUploadUseCase;
import com.suchika.wealth.ports.input.UploadResult;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
import com.suchika.wealth.ports.output.UploadErrorLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.*;

@ApplicationScoped
public class StatementUploadService implements StatementUploadUseCase {

    private static final String UPLOAD_NOT_FOUND = "Upload not found: ";

    private final StatementUploadRepository uploadRepo;
    private final TransactionRepository txnRepo;
    private final AccountRepository accountRepo;
    private final StatementCsvParser csvParser;
    private final UploadErrorLogRepository errorLogRepo;

    public StatementUploadService(StatementUploadRepository uploadRepo,
                                   TransactionRepository txnRepo,
                                   AccountRepository accountRepo,
                                   StatementCsvParser csvParser,
                                   UploadErrorLogRepository errorLogRepo) {
        this.uploadRepo = uploadRepo;
        this.txnRepo = txnRepo;
        this.accountRepo = accountRepo;
        this.csvParser = csvParser;
        this.errorLogRepo = errorLogRepo;
    }

    @Override
    @Transactional
    public UploadResult uploadStatement(UUID accountId, String fileName, String csvContent) {
        accountRepo.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

        StatementUpload upload = StatementUpload.builder()
                .accountId(accountId)
                .fileName(fileName)
                .status(UploadStatus.PENDING)
                .build();
        upload = uploadRepo.save(upload);
        final UUID uploadId = upload.getId();

        try {
            List<ParsedRow> rows = csvParser.parse(csvContent);
            List<ParsedRow> dedupedRows = deduplicateSameFile(rows);

            int insertedCount = 0;
            List<UploadResult.SkippedRow> skipped = new ArrayList<>();
            // Tracks dedup keys inserted in THIS batch to avoid double-checking same-file variants.
            Set<String> insertedThisBatch = new LinkedHashSet<>();

            for (ParsedRow row : dedupedRows) {
                String dedupKey = row.date() + "|" + row.amount().toPlainString() + "|" + row.txnType();
                if (txnRepo.existsByDeduplicationKey(accountId, row.date(), row.amount(), row.txnType())
                        && !insertedThisBatch.contains(dedupKey)) {
                    skipped.add(new UploadResult.SkippedRow(row.date(), row.amount(), row.description()));
                } else {
                    txnRepo.save(Transaction.builder()
                            .accountId(accountId).uploadId(uploadId)
                            .txnDate(row.date()).amount(row.amount())
                            .txnType(row.txnType()).description(row.description())
                            .build());
                    insertedThisBatch.add(dedupKey);
                    insertedCount++;
                }
            }

            if (!skipped.isEmpty()) {
                AppLogger.info("Skipped %d duplicate transactions for upload %s (cross-file dedup)", skipped.size(), uploadId);
            }
            AppLogger.info("Upload %s complete: %d rows saved, %d skipped", uploadId, insertedCount, skipped.size());

            StatementUpload completed = uploadRepo.updateStatus(uploadId, UploadStatus.SUCCESS);
            return new UploadResult(completed, insertedCount, skipped);

        } catch (CsvParseException e) {
            errorLogRepo.save(uploadId, e.getErrorType(), e.getMissingColumns(), e.getMessage());
            uploadRepo.updateStatus(uploadId, UploadStatus.FAILED);
            AppLogger.warn("CSV parse failed for upload %s: %s", uploadId, e.getMessage());
            throw e;
        } catch (Exception e) {
            uploadRepo.updateStatus(uploadId, UploadStatus.FAILED);
            throw e;
        }
    }

    @Override
    @Transactional
    public void rollbackUpload(UUID uploadId) {
        uploadRepo.findById(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_NOT_FOUND + uploadId));
        uploadRepo.delete(uploadId);
        AppLogger.info("Upload %s rolled back — all child transactions removed", uploadId);
    }

    @Override
    public StatementUpload getUpload(UUID uploadId) {
        return uploadRepo.findById(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_NOT_FOUND + uploadId));
    }

    @Override
    public List<StatementUpload> listUploads(UUID accountId) {
        return uploadRepo.findByAccountId(accountId);
    }

    @Override
    public List<UploadErrorLog> getUploadErrors(UUID uploadId) {
        return errorLogRepo.findByUploadId(uploadId);
    }

    /**
     * Same-file deduplication: rows within the same batch that share
     * (date, amount, txnType, description) have a sequence suffix appended to
     * the description so the DB unique constraint treats them as distinct events.
     */
    private List<ParsedRow> deduplicateSameFile(List<ParsedRow> rows) {
        Map<String, Integer> seen = new LinkedHashMap<>();
        List<ParsedRow> result = new ArrayList<>(rows.size());

        for (ParsedRow row : rows) {
            String key = row.date() + "|" + row.amount().toPlainString() + "|" + row.txnType() + "|" + row.description();
            int count = seen.getOrDefault(key, 0);
            seen.put(key, count + 1);

            if (count > 0) {
                result.add(row.withDescription(row.description() + " #" + (count + 1)));
            } else {
                result.add(row);
            }
        }
        return result;
    }
}
