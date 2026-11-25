package com.beloveddesigns.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montants des messages WhatsApp : 28500 → « 28 500 FCFA » (espace simple, lisible partout). */
public final class FcfaFormat {

    private FcfaFormat() {
    }

    public static String money(BigDecimal amount, String currency) {
        String digits = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        StringBuilder grouped = new StringBuilder(digits);
        for (int i = grouped.length() - 3; i > 0; i -= 3) {
            grouped.insert(i, ' ');
        }
        return (negative ? "-" : "") + grouped + " " + currency;
    }
}
