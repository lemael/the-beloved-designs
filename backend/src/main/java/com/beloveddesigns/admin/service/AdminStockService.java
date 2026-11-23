package com.beloveddesigns.admin.service;

import com.beloveddesigns.admin.dto.StockSummaryResponse;
import com.beloveddesigns.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bilan de stock. Référence : products.stock_quantity (stock total du produit).
 * Rupture = stock <= 0 ; stock bas = 1..seuil.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStockService {

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;

    public StockSummaryResponse summary(int threshold) {
        ProductRepository.StockTotalsView totals = productRepository.stockTotals();

        List<StockSummaryResponse.CategoryStock> byCategory = productRepository.stockByCategory().stream()
                .map(c -> new StockSummaryResponse.CategoryStock(
                        c.getSlug(), c.getName(),
                        toLong(c.getProductCount()), toLong(c.getUnits()), toLong(c.getOutOfStockCount())))
                .toList();

        List<StockSummaryResponse.LowStockProduct> lowStock = productRepository
                .findTop50ByStockQuantityLessThanEqualOrderByStockQuantityAscNameAsc(threshold).stream()
                .map(p -> new StockSummaryResponse.LowStockProduct(
                        p.getId(), p.getName(), p.getSlug(), p.getStockQuantity(), p.isActive()))
                .toList();

        return new StockSummaryResponse(
                toLong(totals.getProductCount()),
                toLong(totals.getUnits()),
                productRepository.countByStockQuantityLessThanEqual(0),
                threshold > 0 ? productRepository.countByStockQuantityBetween(1, threshold) : 0,
                threshold,
                byCategory,
                lowStock
        );
    }

    static long toLong(Number n) {
        return n == null ? 0 : n.longValue();
    }
}
