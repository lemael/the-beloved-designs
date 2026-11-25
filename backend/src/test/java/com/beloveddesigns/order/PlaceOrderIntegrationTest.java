package com.beloveddesigns.order;

import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.support.AbstractApiIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Passer la commande » (POST /api/orders) — spécification des règles validées par l'utilisateur
 * le 2026-11-25 (voir ai-memory/memory/decisions.md) :
 *
 * <ul>
 *   <li>Le client donne ville + quartier + numéro WhatsApp que le livreur appellera.</li>
 *   <li>La commande est créée au statut PENDING, le stock est décrémenté, le panier est vidé, le tout
 *       en une seule transaction (tout ou rien).</li>
 *   <li>Pas de paiement en ligne : la réponse contient un lien WhatsApp (wa.me) vers la boutique avec
 *       le détail de la commande pré-rempli.</li>
 *   <li>Prix et stock sont <b>revérifiés au moment de la commande</b> (une promo peut finir, le stock
 *       peut baisser entre l'ajout au panier et la commande).</li>
 *   <li>Première commande : -10 % sur le sous-total des articles (déjà au prix promo, cumulable),
 *       jamais sur la livraison.</li>
 *   <li>Livraison : gratuite si le sous-total après réduction atteint 50 000 FCFA ; sinon Douala
 *       1 500, Yaoundé 2 000, autres villes 3 500 (ville comparée sans tenir compte de la casse,
 *       des accents ni des espaces).</li>
 * </ul>
 */
class PlaceOrderIntegrationTest extends AbstractApiIntegrationTest {

    private static final String CUSTOMER_PHONE = "+237699112233";

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        token = registerCustomer();
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("Commande réussie")
    class Success {

        @Test
        @DisplayName("Première commande à Douala : -10 %, livraison 1 500, statut PENDING, montants détaillés")
        void firstOrderInDouala() throws Exception {
            Product robe = product("15000", 5);
            addToCart(token, robe.getId(), 2).andExpect(status().isCreated());   // 30 000

            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.subtotal").value(30000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(3000))
                    .andExpect(jsonPath("$.deliveryFee").value(1500))
                    .andExpect(jsonPath("$.totalAmount").value(28500))
                    .andExpect(jsonPath("$.currency").value("FCFA"))
                    .andExpect(jsonPath("$.delivery.city").value("Douala"))
                    .andExpect(jsonPath("$.delivery.district").value("Akwa"))
                    .andExpect(jsonPath("$.delivery.contactPhone").value(CUSTOMER_PHONE))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].productId").value(robe.getId()))
                    .andExpect(jsonPath("$.items[0].productName").value(robe.getName()))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.items[0].unitPrice").value(15000))
                    .andExpect(jsonPath("$.items[0].lineTotal").value(30000));
        }

        @Test
        @DisplayName("Le stock est décrémenté et le panier vidé")
        void decrementsStockAndEmptiesCart() throws Exception {
            Product robe = product("15000", 5);
            addToCart(token, robe.getId(), 2).andExpect(status().isCreated());

            placeOrder("Douala", "Akwa", CUSTOMER_PHONE).andExpect(status().isCreated());

            assertThat(stockOf(robe)).isEqualTo(3);
            cart(token).andExpect(jsonPath("$.items.length()").value(0))
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("Lien WhatsApp vers la boutique, message pré-rempli avec n° de commande, articles, total et livraison")
        void returnsWhatsappLinkToShop() throws Exception {
            Product robe = product("15000", 5);
            addToCart(token, robe.getId(), 2).andExpect(status().isCreated());

            String body = placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Number id = JsonPath.read(body, "$.id");
            URI link = URI.create(JsonPath.read(body, "$.whatsappUrl"));
            assertThat(link.getScheme()).isEqualTo("https");
            assertThat(link.getHost()).isEqualTo("wa.me");
            // wa.me attend le numéro sans « + » ni espaces
            assertThat(link.getPath()).isEqualTo("/" + ADMIN_WHATSAPP.substring(1));

            String message = whatsappText(link);
            assertThat(message)
                    .contains("Commande n°" + id)
                    .contains(robe.getName())
                    .contains("× 2")
                    .contains("28 500 FCFA")      // total à payer
                    .contains("Douala")
                    .contains("Akwa")
                    .contains(CUSTOMER_PHONE);
        }

        @Test
        @DisplayName("Le client retrouve sa commande (GET /api/orders/{id})")
        void customerCanReadOwnOrder() throws Exception {
            Product robe = product("15000", 5);
            addToCart(token, robe.getId(), 1).andExpect(status().isCreated());
            Number id = JsonPath.read(placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andReturn().getResponse().getContentAsString(), "$.id");

            mvc.perform(get("/api/orders/" + id).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(15000));   // 15 000 - 1 500 + 1 500
        }

        @Test
        @DisplayName("Un autre client ne voit pas la commande : 404")
        void otherCustomerCannotReadOrder() throws Exception {
            Product robe = product("15000", 5);
            addToCart(token, robe.getId(), 1).andExpect(status().isCreated());
            Number id = JsonPath.read(placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andReturn().getResponse().getContentAsString(), "$.id");

            String otherToken = registerCustomer();
            mvc.perform(get("/api/orders/" + id).header("Authorization", bearer(otherToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("Réduction de première commande (-10 % sur les articles)")
    class FirstOrderDiscount {

        @Test
        @DisplayName("Deuxième commande : plus de réduction")
        void secondOrderHasNoDiscount() throws Exception {
            Product robe = product("15000", 10);
            addToCart(token, robe.getId(), 1).andExpect(status().isCreated());
            placeOrder("Douala", "Akwa", CUSTOMER_PHONE).andExpect(status().isCreated());

            addToCart(token, robe.getId(), 2).andExpect(status().isCreated());   // 30 000
            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstOrderDiscount").value(0))
                    .andExpect(jsonPath("$.deliveryFee").value(1500))
                    .andExpect(jsonPath("$.totalAmount").value(31500));
        }

        @Test
        @DisplayName("Cumulable avec une promo produit : 10 000 -10 % promo = 9 000, puis -10 % = 8 100")
        void stacksWithProductPromotion() throws Exception {
            Product sac = product("10000", 5);
            promotion(sac, new BigDecimal("10"));
            addToCart(token, sac.getId(), 1).andExpect(status().isCreated());

            placeOrder("Douala", "Bonapriso", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items[0].unitPrice").value(9000))
                    .andExpect(jsonPath("$.subtotal").value(9000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(900))
                    .andExpect(jsonPath("$.deliveryFee").value(1500))
                    .andExpect(jsonPath("$.totalAmount").value(9600));
        }

        @Test
        @DisplayName("Jamais sur la livraison : -10 % calculés sur les articles seulement")
        void neverAppliedToDelivery() throws Exception {
            Product p = product("20000", 5);
            addToCart(token, p.getId(), 1).andExpect(status().isCreated());

            placeOrder("Bafoussam", "Centre", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstOrderDiscount").value(2000))   // pas 2 350
                    .andExpect(jsonPath("$.deliveryFee").value(3500))
                    .andExpect(jsonPath("$.totalAmount").value(21500));
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("Frais de livraison")
    class DeliveryFee {

        @Test
        @DisplayName("Yaoundé : 2 000")
        void yaounde() throws Exception {
            assertDeliveryFee("Yaoundé", 2000);
        }

        @Test
        @DisplayName("Autre ville (Bafoussam) : 3 500")
        void otherCity() throws Exception {
            assertDeliveryFee("Bafoussam", 3500);
        }

        @Test
        @DisplayName("Ville saisie sans majuscule, sans accent ou avec espaces : même tarif")
        void cityIsNormalized() throws Exception {
            assertDeliveryFee("  douala ", 1500);
        }

        @Test
        @DisplayName("« Yaounde » sans accent : tarif de Yaoundé")
        void cityWithoutAccent() throws Exception {
            assertDeliveryFee("yaounde", 2000);
        }

        @Test
        @DisplayName("Sous-total après réduction ≥ 50 000 : livraison gratuite")
        void freeDeliveryFrom50000() throws Exception {
            Product p = product("30000", 5);
            addToCart(token, p.getId(), 2).andExpect(status().isCreated());   // 60 000 - 6 000 = 54 000

            placeOrder("Bafoussam", "Centre", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subtotal").value(60000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(6000))
                    .andExpect(jsonPath("$.deliveryFee").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(54000));
        }

        @Test
        @DisplayName("Seuil de 50 000 calculé APRÈS réduction : 55 000 - 5 500 = 49 500 → livraison payante")
        void thresholdAppliesAfterDiscount() throws Exception {
            Product p = product("55000", 5);
            addToCart(token, p.getId(), 1).andExpect(status().isCreated());

            placeOrder("Yaoundé", "Bastos", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.firstOrderDiscount").value(5500))
                    .andExpect(jsonPath("$.deliveryFee").value(2000))
                    .andExpect(jsonPath("$.totalAmount").value(51500));
        }

        @Test
        @DisplayName("Exactement 50 000 après réduction : livraison gratuite")
        void exactly50000IsFree() throws Exception {
            // Deuxième commande (pas de réduction) pour tomber pile sur 50 000
            Product first = product("1000", 5);
            addToCart(token, first.getId(), 1).andExpect(status().isCreated());
            placeOrder("Douala", "Akwa", CUSTOMER_PHONE).andExpect(status().isCreated());

            Product p = product("25000", 5);
            addToCart(token, p.getId(), 2).andExpect(status().isCreated());
            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.deliveryFee").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(50000));
        }

        private void assertDeliveryFee(String city, int expectedFee) throws Exception {
            Product p = product("10000", 5);
            addToCart(token, p.getId(), 1).andExpect(status().isCreated());   // 10 000 - 1 000 = 9 000

            placeOrder(city, "Quartier test", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.deliveryFee").value(expectedFee))
                    .andExpect(jsonPath("$.totalAmount").value(9000 + expectedFee));
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("Revérifications au moment de la commande")
    class Rechecks {

        @Test
        @DisplayName("Promo commencée après l'ajout au panier : le prix du jour est facturé")
        void chargesTodaysPrice() throws Exception {
            Product p = product("10000", 5);
            addToCart(token, p.getId(), 1).andExpect(status().isCreated());   // 10 000 dans le panier
            promotion(p, new BigDecimal("20"));                               // puis -20 % → 8 000

            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items[0].unitPrice").value(8000))
                    .andExpect(jsonPath("$.subtotal").value(8000));
        }

        @Test
        @DisplayName("Stock baissé entre-temps : 409 INSUFFICIENT_STOCK, rien n'est modifié (tout ou rien)")
        void stockDroppedMeanwhile() throws Exception {
            Product ok = product("5000", 10);
            Product rare = product("8000", 3);
            addToCart(token, ok.getId(), 2).andExpect(status().isCreated());
            addToCart(token, rare.getId(), 3).andExpect(status().isCreated());
            setStock(rare, 1);   // vendu ailleurs entre-temps

            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

            assertThat(stockOf(ok)).isEqualTo(10);    // pas décrémenté
            assertThat(stockOf(rare)).isEqualTo(1);
            cart(token).andExpect(jsonPath("$.items.length()").value(2));   // panier intact
        }

        @Test
        @DisplayName("Produit désactivé entre-temps : 409 PRODUCT_UNAVAILABLE, rien n'est modifié")
        void productDeactivatedMeanwhile() throws Exception {
            Product p = product("5000", 10);
            addToCart(token, p.getId(), 1).andExpect(status().isCreated());
            p = productRepository.findById(p.getId()).orElseThrow();
            p.setActive(false);
            productRepository.save(p);

            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));

            assertThat(stockOf(p)).isEqualTo(10);
            cart(token).andExpect(jsonPath("$.items.length()").value(1));
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Nested
    @DisplayName("Requêtes refusées")
    class Rejected {

        @Test
        @DisplayName("Panier vide : 400 EMPTY_CART")
        void emptyCart() throws Exception {
            placeOrder("Douala", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EMPTY_CART"));
        }

        @Test
        @DisplayName("Ville manquante : 400 VALIDATION_ERROR")
        void missingCity() throws Exception {
            fillCart();
            placeOrder("", "Akwa", CUSTOMER_PHONE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Quartier manquant : 400 VALIDATION_ERROR")
        void missingDistrict() throws Exception {
            fillCart();
            placeOrder("Douala", " ", CUSTOMER_PHONE)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Numéro WhatsApp invalide (pas au format international) : 400 VALIDATION_ERROR")
        void invalidPhone() throws Exception {
            fillCart();
            placeOrder("Douala", "Akwa", "699112233")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Visiteur non connecté : 401")
        void anonymous() throws Exception {
            mvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(orderJson("Douala", "Akwa", CUSTOMER_PHONE)))
                    .andExpect(status().isUnauthorized());
        }

        private void fillCart() throws Exception {
            addToCart(token, product("5000", 5).getId(), 1).andExpect(status().isCreated());
        }
    }

    // --- Outils -----------------------------------------------------------------------------------

    private ResultActions placeOrder(String city, String district, String contactPhone) throws Exception {
        return mvc.perform(post("/api/orders")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(city, district, contactPhone)));
    }

    private static String orderJson(String city, String district, String contactPhone) {
        return """
                {"deliveryCity": "%s", "deliveryDistrict": "%s", "contactPhone": "%s"}
                """.formatted(city, district, contactPhone);
    }

    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    private void setStock(Product product, int stock) {
        Product fresh = productRepository.findById(product.getId()).orElseThrow();
        fresh.setStockQuantity(stock);
        productRepository.save(fresh);
    }

    /** Texte du paramètre ?text= d'un lien wa.me, décodé. */
    private static String whatsappText(URI link) {
        String query = link.getRawQuery();
        assertThat(query).startsWith("text=");
        return URLDecoder.decode(query.substring("text=".length()), StandardCharsets.UTF_8);
    }
}
