package com.beloveddesigns.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record YearlyReportResponse(
        int year,
        BigDecimal revenue,
        long orderCount,
        BigDecimal averageOrderValue,
        String currency,
        List<Month> months,
        List<TopProduct> topProducts,
        List<CategorySales> byCategory
) {
    /** month : 1 à 12 — toujours 12 entrées, à 0 pour les mois sans vente. */
    public record Month(int month, long orderCount, BigDecimal revenue) {
    }

    public record TopProduct(Long productId, String productName, long quantity, BigDecimal revenue) {
    }

    public record CategorySales(String slug, String name, long quantity, BigDecimal revenue) {
    }
}
