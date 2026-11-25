package com.beloveddesigns.admin.dto;

import com.beloveddesigns.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

/** Commande du site chargée dans le formulaire « Confirmation paiement » (saisie du n° de commande). */
public record OrderPaymentPrefillResponse(
        Long orderId,
        OrderStatus status,
        String customerWhatsapp,
        String deliveryCity,
        String deliveryDistrict,
        BigDecimal deliveryFee,
        BigDecimal subtotal,
        BigDecimal firstOrderDiscount,
        BigDecimal total,
        List<Item> items
) {

    /** {@code categorySlug} : première catégorie du produit, pour pré-sélectionner la liste « catégorie ». */
    public record Item(Long productId, String productName, String categorySlug, Integer quantity, BigDecimal unitPrice) {
    }
}
