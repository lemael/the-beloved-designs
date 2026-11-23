package com.beloveddesigns.admin.dto;

import com.beloveddesigns.restock.entity.RestockStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RestockResponse(
        Long id,
        LocalDate expectedDate,
        Instant receivedAt,
        String supplier,
        RestockStatus status,
        String notes,
        Instant createdAt,
        int totalQuantity,
        BigDecimal totalCost,
        List<Item> items
) {
    public record Item(
            Long id,
            Long productId,
            String productName,
            Long variantId,
            String variantLabel,
            int quantity,
            BigDecimal unitCost
    ) {
    }
}
