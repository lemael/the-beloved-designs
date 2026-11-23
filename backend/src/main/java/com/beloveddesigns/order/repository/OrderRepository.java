package com.beloveddesigns.order.repository;

import com.beloveddesigns.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    // ---- Bilan annuel (back-office admin) ----
    // Seules les commandes payées / expédiées / livrées comptent dans le chiffre d'affaires.
    // Requêtes natives : regroupement par mois au fuseau du Cameroun (AT TIME ZONE).

    String SOLD_STATUSES = "('PAID', 'SHIPPED', 'DELIVERED')";

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) AS orderCount, COALESCE(SUM(o.total_amount), 0) AS revenue
            FROM orders o
            WHERE o.status IN """ + SOLD_STATUSES + """
              AND o.created_at >= :start AND o.created_at < :end
            """)
    SalesTotalsView salesTotals(@Param("start") Instant start, @Param("end") Instant end);

    @Query(nativeQuery = true, value = """
            SELECT CAST(EXTRACT(MONTH FROM o.created_at AT TIME ZONE :zone) AS INTEGER) AS month,
                   COUNT(*) AS orderCount,
                   COALESCE(SUM(o.total_amount), 0) AS revenue
            FROM orders o
            WHERE o.status IN """ + SOLD_STATUSES + """
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY 1
            ORDER BY 1
            """)
    List<MonthlySalesView> monthlySales(@Param("start") Instant start, @Param("end") Instant end,
                                        @Param("zone") String zone);

    @Query(nativeQuery = true, value = """
            SELECT oi.product_id AS productId,
                   MAX(oi.product_name) AS productName,
                   SUM(oi.quantity) AS quantity,
                   SUM(oi.quantity * oi.unit_price) AS revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.status IN """ + SOLD_STATUSES + """
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY oi.product_id
            ORDER BY revenue DESC
            LIMIT :limit
            """)
    List<ProductSalesView> topProducts(@Param("start") Instant start, @Param("end") Instant end,
                                       @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            SELECT c.slug AS slug, c.name AS name,
                   SUM(oi.quantity) AS quantity,
                   SUM(oi.quantity * oi.unit_price) AS revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN product_categories pc ON pc.product_id = oi.product_id
            JOIN categories c ON c.id = pc.category_id
            WHERE o.status IN """ + SOLD_STATUSES + """
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY c.slug, c.name
            ORDER BY revenue DESC
            """)
    List<CategorySalesView> salesByCategory(@Param("start") Instant start, @Param("end") Instant end);

    interface SalesTotalsView {
        Number getOrderCount();

        Number getRevenue();
    }

    interface MonthlySalesView {
        Number getMonth();

        Number getOrderCount();

        Number getRevenue();
    }

    interface ProductSalesView {
        Number getProductId();

        String getProductName();

        Number getQuantity();

        Number getRevenue();
    }

    interface CategorySalesView {
        String getSlug();

        String getName();

        Number getQuantity();

        Number getRevenue();
    }
}
