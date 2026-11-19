package com.example.domain;

public enum OrderStatus {
    PENDING("Pending", "badge-pending"),
    PROSES("Proses", "badge-proses"),
    SELESAI("Selesai", "badge-selesai"),
    DIAMBIL("Diambil", "badge-diambil");

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public OrderStatus next() {
        return switch (this) {
            case PENDING -> PROSES;
            case PROSES -> SELESAI;
            case SELESAI -> DIAMBIL;
            case DIAMBIL -> DIAMBIL;
        };
    }
}
