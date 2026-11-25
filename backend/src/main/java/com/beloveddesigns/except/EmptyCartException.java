package com.beloveddesigns.except;

/** Commande demandée avec un panier vide → 400 EMPTY_CART. */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super(ErrorMessages.EMPTY_CART);
    }
}
