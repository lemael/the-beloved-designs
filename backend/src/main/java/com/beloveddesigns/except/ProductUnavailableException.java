package com.beloveddesigns.except;

/** Produit du panier désactivé entre l'ajout et la commande → 409 PRODUCT_UNAVAILABLE. */
public class ProductUnavailableException extends RuntimeException {

    public ProductUnavailableException(String productName) {
        super(ErrorMessages.productUnavailable(productName));
    }
}
