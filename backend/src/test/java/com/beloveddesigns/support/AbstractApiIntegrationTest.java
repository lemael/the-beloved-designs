package com.beloveddesigns.support;

import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.repository.ProductRepository;
import com.beloveddesigns.promotion.entity.Promotion;
import com.beloveddesigns.promotion.repository.PromotionRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests qui appellent l'API HTTP (MockMvc : filtres de sécurité, validation, gestion des
 * erreurs compris), avec de quoi créer un client, des produits, des promotions et remplir un panier.
 */
@AutoConfigureMockMvc
public abstract class AbstractApiIntegrationTest extends AbstractIntegrationTest {

    /** Suffixe unique (numéros, slugs) : la base est partagée par tous les tests. */
    protected static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected PromotionRepository promotionRepository;

    /** Inscrit un nouveau client par l'API publique et renvoie son jeton d'accès. */
    protected String registerCustomer() throws Exception {
        String phone = "+2376" + String.format("%08d", SEQUENCE.incrementAndGet());
        String body = """
                {"phone": "%s", "password": "motdepasse-test", "firstName": "Test", "lastName": "Client"}
                """.formatted(phone);
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Produit actif, sans catégorie, au prix et au stock donnés. */
    protected Product product(BigDecimal price, int stock) {
        long n = SEQUENCE.incrementAndGet();
        return productRepository.save(Product.builder()
                .name("Produit test " + n)
                .slug("produit-test-" + n)
                .price(price)
                .stockQuantity(stock)
                .build());
    }

    protected Product product(String price, int stock) {
        return product(new BigDecimal(price), stock);
    }

    /** Promotion en cours (commencée hier, finit demain) sur un produit. */
    protected void promotion(Product product, BigDecimal discountPercent) {
        promotionRepository.save(Promotion.builder()
                .label("Promo test " + SEQUENCE.incrementAndGet())
                .discountPercent(discountPercent)
                .startsAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .endsAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .products(Set.of(product))
                .build());
    }

    protected ResultActions addToCart(String token, Long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productId": %d, "quantity": %d}
                        """.formatted(productId, quantity)));
    }

    protected ResultActions cart(String token) throws Exception {
        return mvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }
}
