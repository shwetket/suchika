package com.suchika.profile.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileTest {

    @Test
    void builder_setsAllFields() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LocalDate dob = LocalDate.of(1990, Month.MAY, 20);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        Profile profile = Profile.builder()
                .id(id)
                .adminId(adminId)
                .fullName("Ketan Verma")
                .dob(dob)
                .relationToAdmin(RelationToAdmin.SELF)
                .emailAddress("ketan@example.com")
                .gender(Gender.MALE)
                .bloodType(BloodType.O_POSITIVE)
                .active(true)
                .createdAt(createdAt)
                .build();

        assertEquals(id, profile.getId());
        assertEquals(adminId, profile.getAdminId());
        assertEquals("Ketan Verma", profile.getFullName());
        assertEquals(dob, profile.getDob());
        assertEquals(RelationToAdmin.SELF, profile.getRelationToAdmin());
        assertEquals("ketan@example.com", profile.getEmailAddress());
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals(BloodType.O_POSITIVE, profile.getBloodType());
        assertEquals(true, profile.isActive());
        assertEquals(createdAt, profile.getCreatedAt());
    }

    @Test
    void builder_defaultsActiveToFalseWhenNotSet() {
        Profile profile = Profile.builder().fullName("Unset Active").build();

        assertFalse(profile.isActive());
    }

    @Test
    void setters_updateFieldsAfterConstruction() {
        Profile profile = new Profile();
        UUID id = UUID.randomUUID();

        profile.setId(id);
        profile.setFullName("Shweta Verma");
        profile.setRelationToAdmin(RelationToAdmin.SPOUSE);
        profile.setActive(true);

        assertEquals(id, profile.getId());
        assertEquals("Shweta Verma", profile.getFullName());
        assertEquals(RelationToAdmin.SPOUSE, profile.getRelationToAdmin());
        assertEquals(true, profile.isActive());
    }

    @Test
    void setAdminId_updatesAdminId() {
        Profile profile = new Profile();
        UUID adminId = UUID.randomUUID();

        profile.setAdminId(adminId);

        assertEquals(adminId, profile.getAdminId());
    }

    @Test
    void setDob_updatesDob() {
        Profile profile = new Profile();
        LocalDate dob = LocalDate.of(1985, Month.MARCH, 12);

        profile.setDob(dob);

        assertEquals(dob, profile.getDob());
    }

    @Test
    void setEmailAddress_updatesEmailAddress() {
        Profile profile = new Profile();

        profile.setEmailAddress("shweta@example.com");

        assertEquals("shweta@example.com", profile.getEmailAddress());
    }

    @Test
    void setGender_updatesGender() {
        Profile profile = new Profile();

        profile.setGender(Gender.FEMALE);

        assertEquals(Gender.FEMALE, profile.getGender());
    }

    @Test
    void setBloodType_updatesBloodType() {
        Profile profile = new Profile();

        profile.setBloodType(BloodType.A_POSITIVE);

        assertEquals(BloodType.A_POSITIVE, profile.getBloodType());
    }

    @Test
    void setCreatedAt_updatesCreatedAt() {
        Profile profile = new Profile();
        Instant createdAt = Instant.parse("2026-02-01T00:00:00Z");

        profile.setCreatedAt(createdAt);

        assertEquals(createdAt, profile.getCreatedAt());
    }
}
