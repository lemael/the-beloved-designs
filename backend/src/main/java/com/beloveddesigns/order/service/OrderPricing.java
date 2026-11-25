package com.beloveddesigns.order.service;

import com.beloveddesigns.order.entity.DeliveryFee;
import com.beloveddesigns.order.repository.DeliveryFeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montants d'une commande (décision du 2026-11-25) :
 * <ol>
 *   <li>réduction de première commande = X % du sous-total des articles (arrondi au FCFA) ;</li>
 *   <li>livraison gratuite si sous-total - réduction ≥ seuil, sinon tarif de la ville
 *       (table delivery_fees, ligne « * » pour les autres villes) ;</li>
 *   <li>total = sous-total - réduction + livraison. La réduction ne porte jamais sur la livraison.</li>
 * </ol>
 */
@Component
public class OrderPricing {

    private final DeliveryFeeRepository deliveryFeeRepository;
    private final BigDecimal freeDeliveryThreshold;
    private final BigDecimal firstOrderDiscountPercent;

    public OrderPricing(DeliveryFeeRepository deliveryFeeRepository,
                        @Value("${app.orders.free-delivery-threshold}") BigDecimal freeDeliveryThreshold,
                        @Value("${app.orders.first-order-discount-percent}") BigDecimal firstOrderDiscountPercent) {
        this.deliveryFeeRepository = deliveryFeeRepository;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
        this.firstOrderDiscountPercent = firstOrderDiscountPercent;
    }

    public Amounts compute(BigDecimal subtotal, boolean firstOrder, String deliveryCity) {
        BigDecimal discount = firstOrder
                ? subtotal.multiply(firstOrderDiscountPercent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal deliveryFee = afterDiscount.compareTo(freeDeliveryThreshold) >= 0
                ? BigDecimal.ZERO
                : deliveryFeeFor(deliveryCity);
        return new Amounts(subtotal, discount, deliveryFee, afterDiscount.add(deliveryFee));
    }

    private BigDecimal deliveryFeeFor(String city) {
        return deliveryFeeRepository.findByCityKey(CityNames.normalize(city))
                .or(() -> deliveryFeeRepository.findByCityKey(DeliveryFee.OTHER_CITIES))
                .map(DeliveryFee::getFee)
                // Table vide (ne devrait pas arriver : remplie par la migration V5) : on refuse plutôt que d'offrir la livraison
                .orElseThrow(() -> new IllegalStateException("Aucun tarif de livraison configuré (table delivery_fees)"));
    }

    public record Amounts(BigDecimal subtotal, BigDecimal firstOrderDiscount, BigDecimal deliveryFee, BigDecimal total) {
    }
}
