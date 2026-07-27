package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.UploadErrorLog;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "upload_error_log", schema = "wealth")
public class UploadErrorLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "upload_id", nullable = false, columnDefinition = "uuid")
    public UUID uploadId;

    @Column(name = "error_type", nullable = false, length = 50)
    public String errorType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "missing_columns", columnDefinition = "text[]")
    public String[] missingColumns;

    @Column(name = "error_detail", nullable = false)
    public String errorDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static UploadErrorLogEntity from(UUID uploadId, String errorType, List<String> missingColumns, String errorDetail) {
        UploadErrorLogEntity e = new UploadErrorLogEntity();
        e.uploadId = uploadId;
        e.errorType = errorType;
        e.missingColumns = missingColumns != null ? missingColumns.toArray(new String[0]) : null;
        e.errorDetail = errorDetail;
        return e;
    }

    public UploadErrorLog toDomain() {
        return UploadErrorLog.builder()
                .id(id)
                .uploadId(uploadId)
                .errorType(errorType)
                .missingColumns(missingColumns != null ? Arrays.asList(missingColumns) : List.of())
                .errorDetail(errorDetail)
                .createdAt(createdAt)
                .build();
    }
}
