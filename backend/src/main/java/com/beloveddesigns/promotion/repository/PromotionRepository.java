package com.beloveddesigns.promotion.repository;

import com.beloveddesigns.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByIsActiveTrue();

    /** Promotions en cours à l'instant donné (actives et dans leurs dates), avec leurs produits actifs. */
    @Query("""
            select distinct p from Promotion p
            left join fetch p.products pr
            where p.isActive = true
              and (p.startsAt is null or p.startsAt <= :now)
              and (p.endsAt is null or p.endsAt >= :now)
            order by p.endsAt asc nulls last
            """)
    List<Promotion> findCurrent(@Param("now") Instant now);

    /**
     * Meilleure réduction en cours par produit (une seule requête pour toute une page de produits).
     * Règle : pas de cumul, la plus forte réduction s'applique.
     */
    @Query("""
            select pr.id as productId, max(p.discountPercent) as discountPercent
            from Promotion p join p.products pr
            where pr.id in :productIds
              and p.isActive = true
              and (p.startsAt is null or p.startsAt <= :now)
              and (p.endsAt is null or p.endsAt >= :now)
            group by pr.id
            """)
    List<ProductDiscountView> findCurrentDiscounts(@Param("productIds") Collection<Long> productIds,
                                                   @Param("now") Instant now);

    interface ProductDiscountView {
        Long getProductId();

        BigDecimal getDiscountPercent();
    }
}
