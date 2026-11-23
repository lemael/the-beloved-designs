package com.beloveddesigns.promotion.dto;

import com.beloveddesigns.product.dto.ProductResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionResponse(
        Long id,
        String label,
        BigDecimal discountPercent,
        Instant startsAt,
        Instant endsAt,
        boolean isActive,
        List<ProductResponse> products
) {
}
