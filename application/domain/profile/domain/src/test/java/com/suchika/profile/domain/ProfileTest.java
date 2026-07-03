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
}
