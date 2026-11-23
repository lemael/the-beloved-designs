package com.beloveddesigns.admin.service;

import com.beloveddesigns.admin.dto.DashboardResponse;
import com.beloveddesigns.admin.dto.YearlyReportResponse;
import com.beloveddesigns.order.repository.OrderRepository;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.restock.entity.RestockStatus;
import com.beloveddesigns.restock.repository.RestockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.beloveddesigns.admin.service.AdminStockService.toLong;

/**
 * Bilans. Commandes prises en compte : PAID, SHIPPED, DELIVERED (voir OrderRepository).
 * Les années et les mois sont découpés au fuseau du Cameroun.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    public static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Douala");
    private static final String CURRENCY = "FCFA";
    private static final int TOP_PRODUCTS_LIMIT = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestockRepository restockRepository;

    public YearlyReportResponse yearly(int year) {
        Instant start = startOfYear(year);
        Instant end = startOfYear(year + 1);

        OrderRepository.SalesTotalsView totals = orderRepository.salesTotals(start, end);
        BigDecimal revenue = toDecimal(totals.getRevenue());
        long orderCount = toLong(totals.getOrderCount());
        BigDecimal average = orderCount > 0
                ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<Integer, OrderRepository.MonthlySalesView> byMonth = new HashMap<>();
        orderRepository.monthlySales(start, end, SHOP_ZONE.getId())
                .forEach(m -> byMonth.put(m.getMonth().intValue(), m));
        List<YearlyReportResponse.Month> months = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> {
                    OrderRepository.MonthlySalesView v = byMonth.get(m);
                    return v == null
                            ? new YearlyReportResponse.Month(m, 0, BigDecimal.ZERO)
                            : new YearlyReportResponse.Month(m, toLong(v.getOrderCount()), toDecimal(v.getRevenue()));
                })
                .toList();

        List<YearlyReportResponse.TopProduct> topProducts = orderRepository
                .topProducts(start, end, TOP_PRODUCTS_LIMIT).stream()
                .map(p -> new YearlyReportResponse.TopProduct(
                        toLong(p.getProductId()), p.getProductName(),
                        toLong(p.getQuantity()), toDecimal(p.getRevenue())))
                .toList();

        List<YearlyReportResponse.CategorySales> byCategory = orderRepository.salesByCategory(start, end).stream()
                .map(c -> new YearlyReportResponse.CategorySales(
                        c.getSlug(), c.getName(), toLong(c.getQuantity()), toDecimal(c.getRevenue())))
                .toList();

        return new YearlyReportResponse(year, revenue, orderCount, average, CURRENCY, months, topProducts, byCategory);
    }

    public DashboardResponse dashboard(int lowStockThreshold) {
        int year = LocalDate.now(SHOP_ZONE).getYear();
        OrderRepository.SalesTotalsView totals = orderRepository.salesTotals(startOfYear(year), startOfYear(year + 1));
        return new DashboardResponse(
                toLong(productRepository.stockTotals().getProductCount()),
                productRepository.countByStockQuantityLessThanEqual(0),
                lowStockThreshold > 0 ? productRepository.countByStockQuantityBetween(1, lowStockThreshold) : 0,
                restockRepository.countByStatus(RestockStatus.PREVU),
                year,
                toDecimal(totals.getRevenue()),
                toLong(totals.getOrderCount()),
                CURRENCY
        );
    }

    private static Instant startOfYear(int year) {
        return LocalDate.of(year, 1, 1).atStartOfDay(SHOP_ZONE).toInstant();
    }

    private static BigDecimal toDecimal(Number n) {
        if (n == null) {
            return BigDecimal.ZERO;
        }
        return n instanceof BigDecimal bd ? bd : new BigDecimal(n.toString());
    }
}
