package com.beloveddesigns.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        Integer stockQuantity,
        boolean featured,
        List<String> imageUrls,
        List<String> categorySlugs
) {
}
