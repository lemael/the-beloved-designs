package com.beloveddesigns.product.service;

import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.product.dto.ProductResponse;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.entity.ProductImage;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    public Page<ProductResponse> findAll(Pageable pageable) {
        return withDiscounts(productRepository.findByIsActiveTrue(pageable));
    }

    public Page<ProductResponse> findFeatured(Pageable pageable) {
        return withDiscounts(productRepository.findByIsActiveTrueAndIsFeaturedTrue(pageable));
    }

    public Page<ProductResponse> findByCategory(String categorySlug, Pageable pageable) {
        return withDiscounts(productRepository.findByIsActiveTrueAndCategories_Slug(categorySlug, pageable));
    }

    public ProductResponse findBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(slug)));
        return toResponse(product);
    }

    /** Un seul produit : réduction en cours comprise. */
    public ProductResponse toResponse(Product product) {
        return toResponse(product, currentDiscounts(List.of(product.getId())).get(product.getId()));
    }

    /** Liste de produits avec leurs réductions en cours (une requête pour toute la liste, pas de N+1). */
    public List<ProductResponse> toResponses(Collection<Product> products) {
        Map<Long, BigDecimal> discounts = currentDiscounts(products.stream().map(Product::getId).toList());
        return products.stream().map(p -> toResponse(p, discounts.get(p.getId()))).toList();
    }

    private Page<ProductResponse> withDiscounts(Page<Product> page) {
        Map<Long, BigDecimal> discounts = currentDiscounts(page.getContent().stream().map(Product::getId).toList());
        return page.map(p -> toResponse(p, discounts.get(p.getId())));
    }

    private Map<Long, BigDecimal> currentDiscounts(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return promotionRepository.findCurrentDiscounts(productIds, Instant.now()).stream()
                .collect(Collectors.toMap(
                        PromotionRepository.ProductDiscountView::getProductId,
                        PromotionRepository.ProductDiscountView::getDiscountPercent));
    }

    /**
     * Prix réellement facturé aujourd'hui : prix promo s'il y a une réduction en cours, sinon prix normal.
     * Même calcul que le prix affiché sur le site (pas de cumul, plus forte réduction, arrondi au FCFA).
     */
    public BigDecimal currentPrice(Product product) {
        BigDecimal discountPercent = currentDiscounts(List.of(product.getId())).get(product.getId());
        BigDecimal promoPrice = promoPrice(product, discountPercent);
        return promoPrice != null ? promoPrice : product.getPrice();
    }

    /** Produit vendable : existe et actif. Un produit désactivé est traité comme introuvable. */
    public Product getActiveProduct(Long productId) {
        Product product = getProduct(productId);
        if (!product.isActive()) {
            throw new ResourceNotFoundException(ErrorMessages.productNotFoundById(productId));
        }
        return product;
    }

    private static BigDecimal promoPrice(Product product, BigDecimal discountPercent) {
        return discountPercent == null ? null : product.getPrice()
                .multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);  // FCFA : pas de centimes
    }

    private ProductResponse toResponse(Product product, BigDecimal discountPercent) {
        BigDecimal promoPrice = promoPrice(product, discountPercent);
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.isFeatured(),
                product.getImages().stream().map(ProductImage::getUrl).toList(),
                product.getCategories().stream().map(c -> c.getSlug()).toList(),
                discountPercent,
                promoPrice
        );
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId.toString())));
    }
}
