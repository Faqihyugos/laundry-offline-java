package com.example.domain;

public enum PaymentMethod {
    CASH("Cash (Tunai)"),
    TRANSFER("Transfer (Bank / E-Wallet)");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
