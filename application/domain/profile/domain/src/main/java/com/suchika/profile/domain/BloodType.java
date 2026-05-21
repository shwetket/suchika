package com.suchika.profile.domain;

public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BloodType fromLabel(String label) {
        if (label == null) return null;
        for (BloodType bt : values()) {
            if (bt.label.equals(label)) return bt;
        }
        throw new IllegalArgumentException("Unknown blood type: " + label);
    }
}
