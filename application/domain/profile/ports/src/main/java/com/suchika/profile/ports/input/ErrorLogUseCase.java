package com.suchika.profile.ports.input;

import com.suchika.profile.domain.ErrorLog;

import java.time.Instant;
import java.util.List;

/**
 * Read side of Phase 4's Application Console error log (ADR-023). The write
 * side is {@code com.suchika.shared.exception.ErrorLogRecorder}, implemented
 * by the same adapter service that backs this use case.
 */
public interface ErrorLogUseCase {

    List<ErrorLog> listErrors(Instant since, int limit);
}
