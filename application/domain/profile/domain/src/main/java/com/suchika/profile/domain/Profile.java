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

    private Profile(Builder builder) {
        this.id = builder.id;
        this.adminId = builder.adminId;
        this.fullName = builder.fullName;
        this.dob = builder.dob;
        this.relationToAdmin = builder.relationToAdmin;
        this.emailAddress = builder.emailAddress;
        this.gender = builder.gender;
        this.bloodType = builder.bloodType;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
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

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder adminId(UUID adminId) { this.adminId = adminId; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder dob(LocalDate dob) { this.dob = dob; return this; }
        public Builder relationToAdmin(RelationToAdmin r) { this.relationToAdmin = r; return this; }
        public Builder emailAddress(String emailAddress) { this.emailAddress = emailAddress; return this; }
        public Builder gender(Gender gender) { this.gender = gender; return this; }
        public Builder bloodType(BloodType bloodType) { this.bloodType = bloodType; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Profile build() { return new Profile(this); }
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
