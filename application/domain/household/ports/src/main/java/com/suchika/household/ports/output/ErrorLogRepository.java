package com.suchika.household.ports.output;

import com.suchika.household.domain.ErrorLog;

import java.time.Instant;
import java.util.List;

public interface ErrorLogRepository {

    void save(String errorCode, int httpStatus, String message, String details);

    List<ErrorLog> findSince(Instant since, int limit);
}
