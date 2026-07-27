package com.suchika.sharedadapter.errorlog;

import com.suchika.shared.errorlog.ErrorLog;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractErrorLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "error_code", nullable = false, length = 50)
    public String errorCode;

    @Column(name = "http_status", nullable = false)
    public int httpStatus;

    @Column(name = "message", nullable = false, length = 500)
    public String message;

    @Column(name = "details", length = 1000)
    public String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void populate(String errorCode, int httpStatus, String message, String details) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
        this.details = details;
    }

    public ErrorLog toDomain() {
        return ErrorLog.builder()
                .id(id)
                .errorCode(errorCode)
                .httpStatus(httpStatus)
                .message(message)
                .details(details)
                .createdAt(createdAt)
                .build();
    }
}
