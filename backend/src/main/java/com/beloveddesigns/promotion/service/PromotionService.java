package com.beloveddesigns.promotion.service;

import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.service.ProductService;
import com.beloveddesigns.promotion.dto.PromotionResponse;
import com.beloveddesigns.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductService productService;

    /** Promotions en cours, avec leurs produits visibles sur le site (produits masqués exclus). */
    public List<PromotionResponse> findCurrent() {
        return promotionRepository.findCurrent(Instant.now()).stream()
                .map(promotion -> {
                    List<Product> products = promotion.getProducts().stream()
                            .filter(Product::isActive)
                            .sorted(Comparator.comparing(Product::getName))
                            .toList();
                    return new PromotionResponse(
                            promotion.getId(),
                            promotion.getLabel(),
                            promotion.getDiscountPercent(),
                            promotion.getStartsAt(),
                            promotion.getEndsAt(),
                            promotion.isActive(),
                            productService.toResponses(products));
                })
                .filter(response -> !response.products().isEmpty())
                .toList();
    }
}
