package com.suchika.shared.errorlog;

import java.time.Instant;
import java.util.UUID;

/**
 * One recorded application error (Phase 4 Application Console, ADR-023).
 * Plain domain object -- populated from a domain's own {@code error_log}
 * table by that domain's adapters layer, never persisted directly here.
 *
 * <p>Moved here from each of the four domains' own {@code domain} packages
 * (2026-07-13 ADR-023 revision) -- the class was byte-for-byte identical in
 * all four, and carries zero domain-specific logic, so it now lives once in
 * {@code shared/}, consistent with that module's documented role as a
 * cross-cutting utility available to every layer including {@code domain}
 * (see {@code DomainRulesTest}'s package-structure javadoc).
 */
public class ErrorLog {

    private final UUID id;
    private final String errorCode;
    private final int httpStatus;
    private final String message;
    private final String details;
    private final Instant createdAt;

    private ErrorLog(Builder builder) {
        this.id = builder.id;
        this.errorCode = builder.errorCode;
        this.httpStatus = builder.httpStatus;
        this.message = builder.message;
        this.details = builder.details;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private String errorCode;
        private int httpStatus;
        private String message;
        private String details;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder httpStatus(int httpStatus) { this.httpStatus = httpStatus; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ErrorLog build() { return new ErrorLog(this); }
    }

    public UUID getId() { return id; }
    public String getErrorCode() { return errorCode; }
    public int getHttpStatus() { return httpStatus; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
