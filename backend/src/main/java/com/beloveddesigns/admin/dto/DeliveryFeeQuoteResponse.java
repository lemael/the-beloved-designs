package com.beloveddesigns.admin.dto;

import java.math.BigDecimal;

/** Tarif de livraison proposé pour une ville et un montant d'articles. */
public record DeliveryFeeQuoteResponse(BigDecimal deliveryFee) {
}
