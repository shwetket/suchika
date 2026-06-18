package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.UploadStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementUploadRepository {

    StatementUpload save(StatementUpload upload);

    Optional<StatementUpload> findById(UUID id);

    StatementUpload updateStatus(UUID id, UploadStatus status);

    List<StatementUpload> findByAccountId(UUID accountId);

    void delete(UUID id);
}
