package com.beloveddesigns.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Tarif de livraison d'une ville (table delivery_fees). La ligne de clé {@link #OTHER_CITIES} vaut pour les autres villes. */
@Entity
@Table(name = "delivery_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryFee {

    public static final String OTHER_CITIES = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de ville normalisé (minuscules, sans accents), voir CityNames.normalize. */
    @Column(name = "city_key", nullable = false, unique = true, length = 100)
    private String cityKey;

    @Column(name = "city_label", nullable = false, length = 100)
    private String cityLabel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee;
}
