package com.beloveddesigns.cart.dto;

import com.beloveddesigns.product.dto.ProductResponse;

public record CartItemResponse(
        Long id,
        ProductResponse product,
        Integer quantity
) {
}
