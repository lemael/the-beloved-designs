package com.beloveddesigns.cart;

import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.support.AbstractApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Ajouter au panier » (POST /api/cart/items), testé de bout en bout : sécurité JWT, validation,
 * service, base PostgreSQL réelle et format des erreurs.
 *
 * <p>Le contenu du panier est vérifié avec GET /api/cart (panier complet + total calculé par le
 * serveur), pour que ces tests restent valables quelle que soit la réponse renvoyée par l'ajout.
 *
 * <p>Isolation : chaque test crée son propre client (numéro unique) et ses propres produits.
 */
class CartAddItemIntegrationTest extends AbstractApiIntegrationTest {

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        token = registerCustomer();
    }

    // --- Cas nominaux ---------------------------------------------------------------------------

    @Test
    @DisplayName("Ajout d'un produit en stock : 201, une ligne dans le panier, au prix du produit")
    void addsProductInStock() throws Exception {
        Product product = product(new BigDecimal("15000"), 10);

        addToCart(token, product.getId(), 1).andExpect(status().isCreated());

        cart(token)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].product.id").value(product.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.total").value(15000));
    }

    @Test
    @DisplayName("Même produit ajouté deux fois : une seule ligne, quantités additionnées")
    void sameProductTwiceMergesIntoOneLine() throws Exception {
        Product product = product(new BigDecimal("5000"), 10);

        addToCart(token, product.getId(), 1).andExpect(status().isCreated());
        addToCart(token, product.getId(), 2).andExpect(status().isCreated());

        cart(token)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(15000));
    }

    // --- Prix -----------------------------------------------------------------------------------

    @Test
    @DisplayName("Produit en promotion : le panier facture le prix promo, pas le prix normal")
    void chargesPromotionalPrice() throws Exception {
        Product product = product(new BigDecimal("10000"), 10);
        promotion(product, new BigDecimal("10"));   // -10 % en cours → 9 000 FCFA

        addToCart(token, product.getId(), 2).andExpect(status().isCreated());

        cart(token)
                .andExpect(jsonPath("$.items[0].unitPrice").value(9000))
                .andExpect(jsonPath("$.items[0].lineTotal").value(18000))
                .andExpect(jsonPath("$.total").value(18000));
    }

    // --- Stock et disponibilité -----------------------------------------------------------------

    @Test
    @DisplayName("Quantité supérieure au stock : 409 INSUFFICIENT_STOCK, rien n'est ajouté")
    void rejectsQuantityAboveStock() throws Exception {
        Product product = product(new BigDecimal("20000"), 2);

        addToCart(token, product.getId(), 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        cart(token).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("Stock dépassé en deux ajouts successifs : le 2e ajout est refusé (409)")
    void rejectsCumulativeQuantityAboveStock() throws Exception {
        Product product = product(new BigDecimal("20000"), 3);

        addToCart(token, product.getId(), 2).andExpect(status().isCreated());
        addToCart(token, product.getId(), 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        cart(token).andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("Produit désactivé : 404, comme s'il n'existait pas")
    void rejectsInactiveProduct() throws Exception {
        Product product = product(new BigDecimal("8000"), 10);
        product.setActive(false);
        productRepository.save(product);

        addToCart(token, product.getId(), 1).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Produit inexistant : 404")
    void rejectsUnknownProduct() throws Exception {
        addToCart(token, 999_999_999L, 1).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Changer la quantité d'une ligne au-delà du stock (PUT) : 409, quantité inchangée")
    void rejectsQuantityUpdateAboveStock() throws Exception {
        Product product = product(new BigDecimal("12000"), 4);
        addToCart(token, product.getId(), 1).andExpect(status().isCreated());
        Number itemId = JsonPath.read(
                cart(token).andReturn().getResponse().getContentAsString(), "$.items[0].id");

        mvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        cart(token).andExpect(jsonPath("$.items[0].quantity").value(1));
    }

    // --- Validation et sécurité -----------------------------------------------------------------

    @Test
    @DisplayName("Quantité 0 : 400 VALIDATION_ERROR")
    void rejectsZeroQuantity() throws Exception {
        Product product = product(new BigDecimal("8000"), 10);

        addToCart(token, product.getId(), 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Visiteur non connecté : 401")
    void rejectsAnonymousVisitor() throws Exception {
        Product product = product(new BigDecimal("8000"), 10);

        mvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(product.getId(), 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // --- Outils ---------------------------------------------------------------------------------

    private static String addItemJson(Long productId, int quantity) {
        return """
                {"productId": %d, "quantity": %d}
                """.formatted(productId, quantity);
    }
}
