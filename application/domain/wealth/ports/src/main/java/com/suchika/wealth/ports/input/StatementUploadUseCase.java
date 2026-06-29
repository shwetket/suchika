package com.suchika.wealth.ports.input;

import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.UploadErrorLog;

import java.util.List;
import java.util.UUID;

public interface StatementUploadUseCase {

    UploadResult uploadStatement(UUID accountId, String fileName, String csvContent);

    void rollbackUpload(UUID uploadId);

    StatementUpload getUpload(UUID uploadId);

    List<StatementUpload> listUploads(UUID accountId);

    List<UploadErrorLog> getUploadErrors(UUID uploadId);
}
