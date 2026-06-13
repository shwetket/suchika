package com.suchika.profile.adapters.persistence;

import com.suchika.profile.domain.BloodType;
import com.suchika.profile.domain.Gender;
import com.suchika.profile.domain.Profile;
import com.suchika.profile.domain.RelationToAdmin;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "profile", schema = "profile")
public class ProfileEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "admin_id", columnDefinition = "uuid")
    public UUID adminId;

    @Column(name = "full_name", nullable = false, length = 150)
    public String fullName;

    @Column(name = "dob", nullable = false)
    public LocalDate dob;

    @Column(name = "relation_to_admin", nullable = false, length = 30)
    public String relationToAdmin;

    @Column(name = "email_address", length = 255)
    public String emailAddress;

    @Column(name = "gender", length = 30)
    public String gender;

    @Column(name = "blood_type", length = 10)
    public String bloodType;

    @Column(name = "is_active", nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static ProfileEntity from(Profile profile) {
        ProfileEntity e = new ProfileEntity();
        e.id = profile.getId();
        e.adminId = profile.getAdminId();
        e.fullName = profile.getFullName();
        e.dob = profile.getDob();
        e.relationToAdmin = profile.getRelationToAdmin() != null ? profile.getRelationToAdmin().name() : null;
        e.emailAddress = profile.getEmailAddress();
        e.gender = profile.getGender() != null ? profile.getGender().name() : null;
        e.bloodType = profile.getBloodType() != null ? profile.getBloodType().getLabel() : null;
        e.active = profile.isActive();
        e.createdAt = profile.getCreatedAt();
        return e;
    }

    public Profile toDomain() {
        return new Profile(
            id,
            adminId,
            fullName,
            dob,
            relationToAdmin != null ? RelationToAdmin.valueOf(relationToAdmin) : null,
            emailAddress,
            gender != null ? Gender.valueOf(gender) : null,
            bloodType != null ? BloodType.fromLabel(bloodType) : null,
            active,
            createdAt
        );
    }
}
