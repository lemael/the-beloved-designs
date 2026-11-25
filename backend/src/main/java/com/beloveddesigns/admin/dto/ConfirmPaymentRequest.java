package com.beloveddesigns.admin.dto;

import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.payment.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Formulaire « Confirmation paiement » du back-office.
 * {@code orderId} renseigné : commande du site ; absent : vente directe.
 */
public record ConfirmPaymentRequest(
        Long orderId,
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = ErrorMessages.INVALID_PHONE_FORMAT)
        String customerWhatsapp,
        @NotEmpty @Valid List<Line> items,
        @Size(max = 100) String deliveryCity,
        @Size(max = 150) String deliveryDistrict,
        @NotNull @PositiveOrZero BigDecimal deliveryFee,
        /** Réduction manuelle en FCFA (0 si absente). */
        @PositiveOrZero BigDecimal discount,
        /** Montant réellement payé (pré-rempli avec le total, modifiable). */
        @NotNull @PositiveOrZero BigDecimal amountPaid,
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate paidOn
) {

    /** Un sous-formulaire produit (catégorie choisie côté écran, seule la sélection du produit compte). */
    public record Line(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }
}
