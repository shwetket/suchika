package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.shared.logging.AppLogger;
import com.suchika.wealth.adapters.services.StatementCsvParser.ParsedRow;
import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.UploadStatus;
import com.suchika.wealth.ports.input.StatementUploadUseCase;
import com.suchika.wealth.ports.output.AccountRepository;
import com.suchika.wealth.ports.output.StatementUploadRepository;
import com.suchika.wealth.ports.output.TransactionRepository;
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

    public StatementUploadService(StatementUploadRepository uploadRepo,
                                   TransactionRepository txnRepo,
                                   AccountRepository accountRepo,
                                   StatementCsvParser csvParser) {
        this.uploadRepo = uploadRepo;
        this.txnRepo = txnRepo;
        this.accountRepo = accountRepo;
        this.csvParser = csvParser;
    }

    @Override
    @Transactional
    public StatementUpload uploadStatement(UUID accountId, String fileName, String csvContent) {
        accountRepo.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

        StatementUpload upload = StatementUpload.builder()
                .accountId(accountId)
                .fileName(fileName)
                .status(UploadStatus.PENDING)
                .build();
        upload = uploadRepo.save(upload);
        UUID uploadId = upload.getId();

        try {
            List<ParsedRow> rows = csvParser.parse(csvContent);
            List<ParsedRow> dedupedRows = deduplicateSameFile(rows);

            int skipped = 0;
            for (ParsedRow row : dedupedRows) {
                if (txnRepo.existsByUniqueKey(accountId, row.date(), row.amount(), row.txnType(), row.description())) {
                    skipped++;
                    continue;
                }
                Transaction txn = Transaction.builder()
                        .accountId(accountId)
                        .uploadId(uploadId)
                        .txnDate(row.date())
                        .amount(row.amount())
                        .txnType(row.txnType())
                        .description(row.description())
                        .build();
                txnRepo.save(txn);
            }

            if (skipped > 0) {
                AppLogger.info("Skipped %d duplicate transactions for upload %s (cross-file dedup)", skipped, uploadId);
            }
            AppLogger.info("Upload %s complete: %d rows saved, %d skipped", uploadId, dedupedRows.size() - skipped, skipped);

            return uploadRepo.updateStatus(uploadId, UploadStatus.SUCCESS);

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
