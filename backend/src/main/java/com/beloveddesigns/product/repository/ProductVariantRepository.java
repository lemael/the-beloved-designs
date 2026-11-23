package com.beloveddesigns.product.repository;

import com.beloveddesigns.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Incrément atomique en base (pas de lecture-modification-écriture côté Java). */
    @Modifying
    @Query("update ProductVariant v set v.stockQuantity = v.stockQuantity + :quantity where v.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
