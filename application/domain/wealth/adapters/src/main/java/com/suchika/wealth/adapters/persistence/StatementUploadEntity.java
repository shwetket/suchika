package com.suchika.wealth.adapters.persistence;

import com.suchika.wealth.domain.StatementUpload;
import com.suchika.wealth.domain.UploadStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statement_upload", schema = "wealth")
public class StatementUploadEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
    public UUID accountId;

    @Column(name = "file_name", nullable = false, length = 255)
    public String fileName;

    @Column(name = "upload_date", nullable = false, updatable = false)
    public Instant uploadDate;

    @Column(name = "status", nullable = false, length = 20)
    public String status = "PENDING";

    @PrePersist
    void onPrePersist() {
        if (uploadDate == null) uploadDate = Instant.now();
    }

    public static StatementUploadEntity from(StatementUpload upload) {
        StatementUploadEntity e = new StatementUploadEntity();
        e.id = upload.getId();
        e.accountId = upload.getAccountId();
        e.fileName = upload.getFileName();
        e.uploadDate = upload.getUploadDate();
        e.status = upload.getStatus() != null ? upload.getStatus().name() : "PENDING";
        return e;
    }

    public StatementUpload toDomain() {
        return StatementUpload.builder()
                .id(id)
                .accountId(accountId)
                .fileName(fileName)
                .uploadDate(uploadDate)
                .status(UploadStatus.valueOf(status))
                .build();
    }
}
