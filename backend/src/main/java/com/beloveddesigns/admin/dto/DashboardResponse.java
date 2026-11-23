package com.beloveddesigns.admin.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalProducts,
        long outOfStockCount,
        long lowStockCount,
        long pendingRestocks,
        int currentYear,
        BigDecimal revenueThisYear,
        long ordersThisYear,
        String currency
) {
}
