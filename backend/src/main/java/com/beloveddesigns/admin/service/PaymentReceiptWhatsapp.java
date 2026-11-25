package com.beloveddesigns.admin.service;

import com.beloveddesigns.order.entity.Order;
import com.beloveddesigns.order.entity.OrderItem;
import com.beloveddesigns.payment.entity.Payment;
import com.beloveddesigns.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import static com.beloveddesigns.order.service.FcfaFormat.money;

/**
 * Reçu de paiement envoyé au CLIENT : lien https://wa.me/&lt;numéro du client&gt;?text=&lt;reçu&gt;.
 * L'admin l'ouvre depuis le back-office ; WhatsApp s'ouvre avec le reçu pré-écrit, il n'a qu'à envoyer.
 */
@Component
public class PaymentReceiptWhatsapp {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String linkFor(Order order, Payment payment, String customerWhatsapp) {
        String digits = customerWhatsapp.replaceAll("\\D", "");
        String text = URLEncoder.encode(message(order, payment), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + digits + "?text=" + text;
    }

    String message(Order order, Payment payment) {
        String currency = order.getCurrency();
        StringBuilder m = new StringBuilder();
        m.append("Bonjour, ici The Beloved Designs. Merci pour votre paiement !\n\n");
        m.append("Reçu — Commande n°").append(order.getId()).append('\n');
        m.append("Date du paiement : ").append(payment.getPaidOn().format(DATE)).append("\n\n");
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            m.append("• ").append(item.getProductName()).append(" × ").append(item.getQuantity())
                    .append(" — ").append(money(lineTotal, currency)).append('\n');
        }
        m.append('\n').append("Sous-total : ").append(money(order.getSubtotal(), currency)).append('\n');
        if (order.getFirstOrderDiscount().signum() > 0) {
            m.append("Réduction première commande : -").append(money(order.getFirstOrderDiscount(), currency)).append('\n');
        }
        if (order.getManualDiscount().signum() > 0) {
            m.append("Réduction : -").append(money(order.getManualDiscount(), currency)).append('\n');
        }
        m.append("Livraison");
        if (order.getDeliveryCity() != null && !order.getDeliveryCity().isBlank()) {
            m.append(" (").append(order.getDeliveryCity()).append(')');
        }
        m.append(" : ").append(order.getDeliveryFee().signum() == 0 ? "offerte" : money(order.getDeliveryFee(), currency)).append('\n');
        m.append("Total : ").append(money(order.getTotalAmount(), currency)).append('\n');
        m.append("Montant payé : ").append(money(payment.getAmount(), currency))
                .append(" (").append(PaymentMethod.valueOf(payment.getProvider()).label()).append(")\n\n");
        m.append("Merci pour votre confiance et à bientôt !");
        return m.toString();
    }
}
