package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.StatementUpload;

import java.util.List;
import java.util.UUID;

public interface StatementUploadUseCase {

    StatementUpload uploadStatement(UUID accountId, String fileName, String csvContent);

    void rollbackUpload(UUID uploadId);

    StatementUpload getUpload(UUID uploadId);

    List<StatementUpload> listUploads(UUID accountId);
}
