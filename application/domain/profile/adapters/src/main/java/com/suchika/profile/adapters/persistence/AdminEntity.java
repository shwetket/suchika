package com.suchika.profile.adapters.persistence;

import com.suchika.profile.domain.Admin;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin", schema = "profile")
public class AdminEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "display_name", nullable = false, length = 150)
    public String displayName;

    @Column(name = "email_address", length = 255)
    public String emailAddress;

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static AdminEntity from(Admin admin) {
        AdminEntity e = new AdminEntity();
        e.id = admin.getId();
        e.displayName = admin.getDisplayName();
        e.emailAddress = admin.getEmailAddress();
        e.active = admin.isActive();
        e.createdAt = admin.getCreatedAt();
        return e;
    }

    public Admin toDomain() {
        return new Admin(id, displayName, emailAddress, active, createdAt);
    }
}
