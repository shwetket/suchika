package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.ExpenseCategory;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.TransactionUseCase;
import com.suchika.wealth.ports.output.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class TransactionService implements TransactionUseCase {

    private static final String TRANSACTION_NOT_FOUND = "Transaction not found: ";

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Transaction> listByAccount(UUID accountId, LocalDate from, LocalDate to, TxnType txnType) {
        // profile_id scoping is not yet part of this use case's HTTP contract (out of Epic 8
        // Phase 1 scope) — null preserves existing unscoped behavior for this call site.
        return repository.findByAccountId(accountId, null, from, to, txnType);
    }

    @Override
    public Transaction getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND + id));
    }

    @Override
    @Transactional
    public Transaction updateCategory(UUID id, ExpenseCategory category) {
        if (category == null) {
            throw new BadRequestException("category is required");
        }
        Transaction txn = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(TRANSACTION_NOT_FOUND + id));

        Map<String, String> metadata = new HashMap<>(
                txn.getMetadata() != null ? txn.getMetadata() : Map.of());
        metadata.put("category", category.name());

        Transaction updated = Transaction.builder()
                .id(txn.getId())
                .accountId(txn.getAccountId())
                .uploadId(txn.getUploadId())
                .txnDate(txn.getTxnDate())
                .amount(txn.getAmount())
                .txnType(txn.getTxnType())
                .description(txn.getDescription())
                .metadata(metadata)
                .createdAt(txn.getCreatedAt())
                .build();
        return repository.save(updated);
    }

    @Override
    @Transactional
    public List<Transaction> bulkUpdateCategory(List<UUID> ids, ExpenseCategory category) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("transaction_ids is required");
        }
        return ids.stream().map(id -> updateCategory(id, category)).toList();
    }
}
