package com.beloveddesigns.order.entity;

import com.beloveddesigns.address.entity.Address;
import com.beloveddesigns.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /** Somme des articles au prix du jour (promo comprise), avant réduction de première commande. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "first_order_discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal firstOrderDiscount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /** subtotal - firstOrderDiscount + deliveryFee : montant à payer. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Livraison, figée sur la commande (saisie par le client au moment de commander)
    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_district", length = 150)
    private String deliveryDistrict;

    /** Numéro WhatsApp que le livreur appellera (peut différer du numéro du compte). */
    @Column(name = "delivery_contact_phone", length = 20)
    private String deliveryContactPhone;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "FCFA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
