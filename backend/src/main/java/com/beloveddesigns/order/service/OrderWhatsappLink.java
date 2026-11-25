package com.beloveddesigns.order.service;

import com.beloveddesigns.order.entity.Order;
import com.beloveddesigns.order.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Lien « Envoyer ma commande sur WhatsApp » : https://wa.me/&lt;numéro de la boutique&gt;?text=&lt;message&gt;.
 * Le client l'ouvre après avoir passé commande ; la boutique reçoit le détail et organise paiement et livraison.
 */
@Slf4j
@Component
public class OrderWhatsappLink {

    private final String adminDigits;

    public OrderWhatsappLink(@Value("${app.orders.admin-whatsapp:}") String adminWhatsapp) {
        // wa.me attend le numéro international sans « + », espaces ni tirets
        this.adminDigits = adminWhatsapp == null ? "" : adminWhatsapp.replaceAll("\\D", "");
        if (adminDigits.isEmpty()) {
            log.warn("ADMIN_WHATSAPP n'est pas configuré : les commandes n'auront pas de lien WhatsApp");
        }
    }

    /** Lien complet, ou null si le numéro de la boutique n'est pas configuré. */
    public String linkFor(Order order) {
        if (adminDigits.isEmpty()) {
            return null;
        }
        // URLEncoder encode les espaces en « + » : WhatsApp attend %20
        String text = URLEncoder.encode(message(order), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + adminDigits + "?text=" + text;
    }

    String message(Order order) {
        StringBuilder m = new StringBuilder();
        m.append("Bonjour The Beloved Designs, je viens de passer une commande sur le site.\n\n");
        m.append("Commande n°").append(order.getId()).append(" :\n");
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            m.append("• ").append(item.getProductName())
                    .append(" × ").append(item.getQuantity())
                    .append(" — ").append(money(lineTotal, order)).append('\n');
        }
        m.append('\n').append("Sous-total : ").append(money(order.getSubtotal(), order)).append('\n');
        if (order.getFirstOrderDiscount().signum() > 0) {
            m.append("Réduction première commande : -").append(money(order.getFirstOrderDiscount(), order)).append('\n');
        }
        m.append("Livraison : ")
                .append(order.getDeliveryFee().signum() == 0 ? "offerte" : money(order.getDeliveryFee(), order))
                .append('\n');
        m.append("Total à payer : ").append(money(order.getTotalAmount(), order)).append("\n\n");
        m.append("Livraison à ").append(order.getDeliveryCity()).append(", quartier ").append(order.getDeliveryDistrict()).append('\n');
        m.append("Numéro à appeler : ").append(order.getDeliveryContactPhone());
        return m.toString();
    }

    /** 28500 → « 28 500 FCFA » (espace simple : lisible partout, y compris dans WhatsApp). */
    static String money(BigDecimal amount, Order order) {
        String digits = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        StringBuilder grouped = new StringBuilder(digits);
        for (int i = grouped.length() - 3; i > 0; i -= 3) {
            grouped.insert(i, ' ');
        }
        return (negative ? "-" : "") + grouped + " " + order.getCurrency();
    }
}
