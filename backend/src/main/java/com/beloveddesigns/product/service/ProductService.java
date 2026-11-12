package com.beloveddesigns.product.service;

import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.product.dto.ProductResponse;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.entity.ProductImage;
import com.beloveddesigns.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(this::toResponse);
    }

    public Page<ProductResponse> findFeatured(Pageable pageable) {
        return productRepository.findByIsActiveTrueAndIsFeaturedTrue(pageable).map(this::toResponse);
    }

    public Page<ProductResponse> findByCategory(String categorySlug, Pageable pageable) {
        return productRepository.findByIsActiveTrueAndCategories_Slug(categorySlug, pageable).map(this::toResponse);
    }

    public ProductResponse findBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(slug)));
        return toResponse(product);
    }

    public ProductResponse toResponse(Product product) {
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
                product.getCategories().stream().map(c -> c.getSlug()).toList()
        );
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId.toString())));
    }
}
