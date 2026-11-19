package com.example.domain;

public enum PaymentStatus {
    LUNAS("Lunas", "badge-lunas"),
    BELUM_LUNAS("Belum Lunas", "badge-belum-lunas"),
    SEBAGIAN("Sebagian", "badge-sebagian");

    private final String displayName;
    private final String badgeClass;

    PaymentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
