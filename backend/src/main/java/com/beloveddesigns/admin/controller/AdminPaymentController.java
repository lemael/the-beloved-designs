package com.beloveddesigns.admin.controller;

import com.beloveddesigns.admin.dto.ConfirmPaymentRequest;
import com.beloveddesigns.admin.dto.DeliveryFeeQuoteResponse;
import com.beloveddesigns.admin.dto.OrderPaymentPrefillResponse;
import com.beloveddesigns.admin.dto.PaymentConfirmationResponse;
import com.beloveddesigns.admin.service.AdminPaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Back-office « Confirmation paiement » (/api/admin/** : rôle ADMIN, voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    /** Commande du site à confirmer : pré-remplit le formulaire. */
    @GetMapping("/orders/{orderId}")
    public OrderPaymentPrefillResponse prefill(@PathVariable Long orderId) {
        return adminPaymentService.prefill(orderId);
    }

    /** Tarif de livraison proposé pour une ville et un montant d'articles (après réductions). */
    @GetMapping("/delivery-fee")
    public DeliveryFeeQuoteResponse deliveryFee(@RequestParam String city,
                                                @RequestParam @PositiveOrZero BigDecimal amount) {
        return adminPaymentService.quoteDeliveryFee(city, amount);
    }

    /** Confirme un paiement (commande du site ou vente directe) et renvoie le lien du reçu WhatsApp. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentConfirmationResponse confirm(@Valid @RequestBody ConfirmPaymentRequest request) {
        return adminPaymentService.confirm(request);
    }
}
