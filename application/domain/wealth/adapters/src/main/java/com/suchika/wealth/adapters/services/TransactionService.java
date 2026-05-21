package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.NotFoundException;
import com.suchika.wealth.domain.Transaction;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.TransactionUseCase;
import com.suchika.wealth.ports.output.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TransactionService implements TransactionUseCase {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Transaction> listByAccount(UUID accountId, LocalDate from, LocalDate to, TxnType txnType) {
        return repository.findByAccountId(accountId, from, to, txnType);
    }

    @Override
    public Transaction getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }
}
