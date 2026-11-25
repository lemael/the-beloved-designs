package com.beloveddesigns.admin.dto;

import com.beloveddesigns.order.dto.OrderResponse;
import com.beloveddesigns.order.entity.OrderStatus;
import com.beloveddesigns.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Paiement confirmé : commande payée, détail des montants et lien WhatsApp du reçu vers le client. */
public record PaymentConfirmationResponse(
        Long orderId,
        Long paymentId,
        OrderStatus status,
        String customerWhatsapp,
        String deliveryCity,
        String deliveryDistrict,
        List<OrderResponse.Item> items,
        BigDecimal subtotal,
        BigDecimal firstOrderDiscount,
        BigDecimal discount,
        BigDecimal deliveryFee,
        BigDecimal total,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        LocalDate paidOn,
        String receiptWhatsappUrl
) {
}
