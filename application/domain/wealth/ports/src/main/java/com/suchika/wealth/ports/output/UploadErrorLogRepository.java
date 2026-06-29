package com.suchika.wealth.ports.output;

import com.suchika.wealth.domain.UploadErrorLog;

import java.util.List;
import java.util.UUID;

public interface UploadErrorLogRepository {

    void save(UUID uploadId, String errorType, List<String> missingColumns, String errorDetail);

    List<UploadErrorLog> findByUploadId(UUID uploadId);
}
