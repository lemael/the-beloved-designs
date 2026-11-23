package com.beloveddesigns.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        Integer stockQuantity,
        boolean featured,
        boolean active,
        List<String> imageUrls,
        List<String> categorySlugs,
        List<VariantResponse> variants,
        Instant createdAt
) {
    public record VariantResponse(Long id, String size, String color, Integer stockQuantity) {
    }
}
