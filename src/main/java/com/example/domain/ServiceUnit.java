package com.example.domain;

public enum ServiceUnit {
    KG("kg (Kilogram)"),
    PCS("pcs (Satuan)");

    private final String displayName;

    ServiceUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
