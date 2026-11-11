package com.beloveddesigns.product.repository;

import com.beloveddesigns.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndIsFeaturedTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategories_Slug(String categorySlug, Pageable pageable);
}
