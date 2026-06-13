package com.suchika.profile.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Profile {

    private UUID id;
    private UUID adminId;
    private String fullName;
    private LocalDate dob;
    private RelationToAdmin relationToAdmin;
    private String emailAddress;
    private Gender gender;
    private BloodType bloodType;
    private boolean active;
    private Instant createdAt;

    public Profile() {}

    public Profile(UUID id, UUID adminId, String fullName, LocalDate dob,
                   RelationToAdmin relationToAdmin, String emailAddress,
                   Gender gender, BloodType bloodType, boolean active, Instant createdAt) {
        this.id = id;
        this.adminId = adminId;
        this.fullName = fullName;
        this.dob = dob;
        this.relationToAdmin = relationToAdmin;
        this.emailAddress = emailAddress;
        this.gender = gender;
        this.bloodType = bloodType;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAdminId() { return adminId; }
    public void setAdminId(UUID adminId) { this.adminId = adminId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public RelationToAdmin getRelationToAdmin() { return relationToAdmin; }
    public void setRelationToAdmin(RelationToAdmin relationToAdmin) { this.relationToAdmin = relationToAdmin; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodType bloodType) { this.bloodType = bloodType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
