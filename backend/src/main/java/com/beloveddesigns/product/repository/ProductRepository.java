package com.beloveddesigns.product.repository;

import com.beloveddesigns.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndIsFeaturedTrue(Pageable pageable);

    Page<Product> findByIsActiveTrueAndCategories_Slug(String categorySlug, Pageable pageable);

    // ---- Back-office admin (produits actifs ET inactifs) ----

    Page<Product> findDistinctByCategories_SlugIn(Collection<String> categorySlugs, Pageable pageable);

    /**
     * Produits verrouillés en écriture (SELECT … FOR UPDATE) jusqu'à la fin de la transaction : deux
     * commandes simultanées ne peuvent pas vendre la même dernière pièce. Triés par id pour que les
     * transactions prennent les verrous dans le même ordre (pas d'interblocage).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    /** Incrément atomique en base (pas de lecture-modification-écriture côté Java). */
    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("select count(p) as productCount, coalesce(sum(p.stockQuantity), 0) as units from Product p")
    StockTotalsView stockTotals();

    long countByStockQuantityLessThanEqual(int quantity);

    long countByStockQuantityBetween(int min, int max);

    List<Product> findTop50ByStockQuantityLessThanEqualOrderByStockQuantityAscNameAsc(int threshold);

    @Query("""
            select c.slug as slug, c.name as name, count(p) as productCount,
                   coalesce(sum(p.stockQuantity), 0) as units,
                   sum(case when p.stockQuantity <= 0 then 1 else 0 end) as outOfStockCount
            from Product p join p.categories c
            group by c.slug, c.name
            order by c.name
            """)
    List<CategoryStockView> stockByCategory();

    interface StockTotalsView {
        Number getProductCount();

        Number getUnits();
    }

    interface CategoryStockView {
        String getSlug();

        String getName();

        Number getProductCount();

        Number getUnits();

        Number getOutOfStockCount();
    }
}
