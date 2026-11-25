package com.beloveddesigns.order.dto;

import com.beloveddesigns.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Commande avec le détail des montants (tous calculés par le serveur) et le lien WhatsApp vers la
 * boutique, message pré-rempli. {@code whatsappUrl} est null si le numéro de la boutique n'est pas
 * configuré (variable ADMIN_WHATSAPP).
 */
public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal firstOrderDiscount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Delivery delivery,
        List<Item> items,
        String whatsappUrl
) {

    public record Delivery(String city, String district, String contactPhone) {
    }

    public record Item(Long productId, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
