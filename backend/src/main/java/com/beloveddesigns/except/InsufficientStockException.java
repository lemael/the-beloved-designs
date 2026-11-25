package com.beloveddesigns.except;

import lombok.Getter;

/** Quantité demandée supérieure au stock disponible → 409 INSUFFICIENT_STOCK. */
@Getter
public class InsufficientStockException extends RuntimeException {

    /** Quantité encore ajoutable (stock moins ce qui est déjà dans le panier), jamais négative. */
    private final int available;

    public InsufficientStockException(int available) {
        super(ErrorMessages.insufficientStock(Math.max(available, 0)));
        this.available = Math.max(available, 0);
    }
}
