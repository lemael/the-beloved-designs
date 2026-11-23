package com.beloveddesigns.admin.dto;

import java.util.List;

public record StockSummaryResponse(
        long totalProducts,
        long totalUnits,
        long outOfStockCount,
        long lowStockCount,
        int lowStockThreshold,
        List<CategoryStock> byCategory,
        List<LowStockProduct> lowStockProducts
) {
    public record CategoryStock(String slug, String name, long productCount, long units, long outOfStockCount) {
    }

    public record LowStockProduct(Long id, String name, String slug, int stockQuantity, boolean active) {
    }
}
