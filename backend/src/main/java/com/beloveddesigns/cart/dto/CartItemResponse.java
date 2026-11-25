package com.beloveddesigns.cart.dto;

import com.beloveddesigns.cart.entity.CartItem;
import com.beloveddesigns.product.dto.ProductResponse;

import java.math.BigDecimal;

/**
 * Ligne du panier. {@code unitPrice} est le prix facturé enregistré à l'ajout (promo comprise) :
 * le frontend l'affiche tel quel et ne recalcule jamais de prix.
 */
public record CartItemResponse(
        Long id,
        ProductResponse product,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static CartItemResponse of(CartItem item, ProductResponse product) {
        return new CartItemResponse(
                item.getId(),
                product,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
