package com.beloveddesigns.product.controller;

import com.beloveddesigns.product.dto.ProductResponse;
import com.beloveddesigns.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> findAll(
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        if (category != null) {
            return productService.findByCategory(category, pageable);
        }
        return productService.findAll(pageable);
    }

    @GetMapping("/featured")
    public Page<ProductResponse> findFeatured(Pageable pageable) {
        return productService.findFeatured(pageable);
    }

    @GetMapping("/{slug}")
    public ProductResponse findBySlug(@PathVariable String slug) {
        return productService.findBySlug(slug);
    }
}
