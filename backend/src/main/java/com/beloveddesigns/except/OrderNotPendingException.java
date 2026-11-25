package com.beloveddesigns.except;

/** Paiement confirmé sur une commande qui n'est plus en attente (déjà payée, annulée…) → 409 ORDER_NOT_PENDING. */
public class OrderNotPendingException extends RuntimeException {

    public OrderNotPendingException(Long orderId, String status) {
        super(ErrorMessages.orderNotPending(orderId, status));
    }
}
