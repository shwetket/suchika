package com.suchika.profile.adapters.persistence;

import com.suchika.shared.errorlog.ErrorLog;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_log", schema = "profile")
public class ErrorLogEntity extends PanacheEntityBase {

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

    public static ErrorLogEntity from(String errorCode, int httpStatus, String message, String details) {
        ErrorLogEntity e = new ErrorLogEntity();
        e.errorCode = errorCode;
        e.httpStatus = httpStatus;
        e.message = message;
        e.details = details;
        return e;
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
