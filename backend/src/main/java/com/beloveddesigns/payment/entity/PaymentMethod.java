package com.beloveddesigns.payment.entity;

/** Modes de paiement proposés dans le back-office (stockés dans payments.provider). */
public enum PaymentMethod {
    CASH("Espèces"),
    MTN_MOMO("MTN Mobile Money"),
    ORANGE_MONEY("Orange Money"),
    BANK_TRANSFER("Virement bancaire");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /** Libellé affiché au client (reçu WhatsApp). */
    public String label() {
        return label;
    }
}
