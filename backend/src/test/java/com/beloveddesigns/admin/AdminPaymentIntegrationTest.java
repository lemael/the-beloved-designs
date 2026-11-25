package com.beloveddesigns.admin;

import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.support.AbstractApiIntegrationTest;
import com.beloveddesigns.user.entity.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Back-office « Confirmation paiement » — spécification des règles validées par l'utilisateur le
 * 2026-11-25 (ai-memory/memory/decisions.md, « Admin : section Confirmation paiement ») :
 *
 * <ul>
 *   <li><b>Commande du site</b> (n° de commande saisi) : formulaire pré-rempli
 *       (GET /api/admin/payments/orders/{id}) ; à la confirmation la commande passe PAID. Si l'admin
 *       change produits ou quantités, le stock est ajusté (rendu / décrémenté, refus si insuffisant).</li>
 *   <li><b>Vente directe</b> (sans n° de commande) : commande PAID créée, stock décrémenté ; le client
 *       est retrouvé par son numéro WhatsApp, ou un compte <b>sans mot de passe utilisable</b> est créé.</li>
 *   <li>Paiement : mode, date, frais de livraison (tarif de la ville proposé, modifiable), réduction
 *       manuelle, montant payé modifiable. Total = sous-total − réduction 1re commande (site) −
 *       réduction manuelle + livraison.</li>
 *   <li>Reçu : lien wa.me vers le WhatsApp du <b>client</b>, reçu pré-rempli.</li>
 * </ul>
 */
class AdminPaymentIntegrationTest extends AbstractApiIntegrationTest {

    private static final String PAID_ON = "2026-11-20";

    private String admin;

    @BeforeEach
    void logInAsAdmin() throws Exception {
        admin = registerAdmin();
    }

    // =============================================================================================
    @Nested
    @DisplayName("Commande passée sur le site")
    class SiteOrder {

        private String customer;
        private Product robe;
        private Product sac;
        private long orderId;

        /** Client : 2 robes à 15 000, livraison Douala → 30 000 − 3 000 + 1 500 = 28 500. Stock robe 5 → 3. */
        @BeforeEach
        void customerPlacesOrder() throws Exception {
            customer = registerCustomer();
            robe = product("15000", 5);
            sac = product("20000", 4);
            addToCart(customer, robe.getId(), 2).andExpect(status().isCreated());
            String body = mvc.perform(post("/api/orders")
                            .header("Authorization", bearer(customer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"deliveryCity": "Douala", "deliveryDistrict": "Akwa", "contactPhone": "+237699112233"}
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            orderId = ((Number) JsonPath.read(body, "$.id")).longValue();
        }

        @Test
        @DisplayName("Pré-remplissage : produits, WhatsApp du client, livraison et montants de la commande")
        void prefillsFormFromOrder() throws Exception {
            mvc.perform(get("/api/admin/payments/orders/" + orderId).header("Authorization", bearer(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.customerWhatsapp").value("+237699112233"))
                    .andExpect(jsonPath("$.deliveryCity").value("Douala"))
                    .andExpect(jsonPath("$.deliveryDistrict").value("Akwa"))
                    .andExpect(jsonPath("$.deliveryFee").value(1500))
                    .andExpect(jsonPath("$.subtotal").value(30000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(3000))
                    .andExpect(jsonPath("$.total").value(28500))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].productId").value(robe.getId()))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.items[0].unitPrice").value(15000));
        }

        @Test
        @DisplayName("Confirmation sans changement : commande PAID, paiement enregistré, stock inchangé")
        void confirmsUnchangedOrder() throws Exception {
            confirm(siteOrderJson(orderId, "[{\"productId\": %d, \"quantity\": 2}]".formatted(robe.getId()),
                    1500, 0, 28500, "MTN_MOMO"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.paymentId").isNumber())
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.subtotal").value(30000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(3000))
                    .andExpect(jsonPath("$.discount").value(0))
                    .andExpect(jsonPath("$.deliveryFee").value(1500))
                    .andExpect(jsonPath("$.total").value(28500))
                    .andExpect(jsonPath("$.amountPaid").value(28500))
                    .andExpect(jsonPath("$.paymentMethod").value("MTN_MOMO"))
                    .andExpect(jsonPath("$.paidOn").value(PAID_ON));

            // Le client voit sa commande payée ; le stock avait déjà été décompté à la commande
            mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(customer)))
                    .andExpect(jsonPath("$.status").value("PAID"));
            assertThat(stockOf(robe)).isEqualTo(3);
        }

        @Test
        @DisplayName("Reçu : lien WhatsApp vers le client avec n° de commande, détail, montant payé, mode et date")
        void receiptLinkToCustomer() throws Exception {
            String body = confirm(siteOrderJson(orderId, "[{\"productId\": %d, \"quantity\": 2}]".formatted(robe.getId()),
                    1500, 0, 28500, "MTN_MOMO"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            URI link = URI.create(JsonPath.read(body, "$.receiptWhatsappUrl"));
            assertThat(link.getHost()).isEqualTo("wa.me");
            assertThat(link.getPath()).isEqualTo("/237699112233");   // le CLIENT, pas la boutique
            assertThat(whatsappText(link))
                    .contains("Reçu")
                    .contains("Commande n°" + orderId)
                    .contains(robe.getName())
                    .contains("× 2")
                    .contains("Montant payé : 28 500 FCFA")
                    .contains("MTN Mobile Money")
                    .contains("20/11/2026");
        }

        @Test
        @DisplayName("Admin modifie la commande (1 robe au lieu de 2, + 1 sac) : stock ajusté, -10 % recalculé")
        void adjustsStockWhenOrderChanges() throws Exception {
            String items = "[{\"productId\": %d, \"quantity\": 1}, {\"productId\": %d, \"quantity\": 1}]"
                    .formatted(robe.getId(), sac.getId());
            // 15 000 + 20 000 = 35 000 ; -10 % = 3 500 ; livraison 1 500 → 33 000
            confirm(siteOrderJson(orderId, items, 1500, 0, 33000, "CASH"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.subtotal").value(35000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(3500))
                    .andExpect(jsonPath("$.total").value(33000));

            assertThat(stockOf(robe)).isEqualTo(4);   // 1 robe rendue
            assertThat(stockOf(sac)).isEqualTo(3);    // 1 sac pris
        }

        @Test
        @DisplayName("Modification au-delà du stock : 409, commande et stock inchangés")
        void rejectsChangeAboveStock() throws Exception {
            String items = "[{\"productId\": %d, \"quantity\": 2}, {\"productId\": %d, \"quantity\": 9}]"
                    .formatted(robe.getId(), sac.getId());

            confirm(siteOrderJson(orderId, items, 1500, 0, 100000, "CASH"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

            assertThat(stockOf(robe)).isEqualTo(3);
            assertThat(stockOf(sac)).isEqualTo(4);
            mvc.perform(get("/api/admin/payments/orders/" + orderId).header("Authorization", bearer(admin)))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items.length()").value(1));
        }

        @Test
        @DisplayName("Commande déjà payée : 409 ORDER_NOT_PENDING")
        void rejectsAlreadyPaidOrder() throws Exception {
            String items = "[{\"productId\": %d, \"quantity\": 2}]".formatted(robe.getId());
            confirm(siteOrderJson(orderId, items, 1500, 0, 28500, "CASH")).andExpect(status().isCreated());

            confirm(siteOrderJson(orderId, items, 1500, 0, 28500, "CASH"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_PENDING"));
        }

        @Test
        @DisplayName("Réduction manuelle et montant payé différent du total : enregistrés tels quels")
        void manualDiscountAndAmountPaid() throws Exception {
            String items = "[{\"productId\": %d, \"quantity\": 2}]".formatted(robe.getId());
            // 30 000 − 3 000 − 500 + 1 500 = 28 000 ; le client paie 28 000
            confirm(siteOrderJson(orderId, items, 1500, 500, 28000, "ORANGE_MONEY"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.discount").value(500))
                    .andExpect(jsonPath("$.total").value(28000))
                    .andExpect(jsonPath("$.amountPaid").value(28000));
        }
    }

    // =============================================================================================
    @Nested
    @DisplayName("Vente directe (sans n° de commande)")
    class DirectSale {

        @Test
        @DisplayName("Nouveau client : compte créé sans mot de passe utilisable, commande PAID, stock décrémenté")
        void newCustomerGetsAccountWithoutPassword() throws Exception {
            String phone = uniquePhone();
            Product robe = product("15000", 5);

            confirm(directSaleJson(phone, "[{\"productId\": %d, \"quantity\": 2}]".formatted(robe.getId()),
                    "Yaoundé", "Bastos", 2000, 1000, 31000, "CASH"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").isNumber())
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.customerWhatsapp").value(phone))
                    .andExpect(jsonPath("$.subtotal").value(30000))
                    .andExpect(jsonPath("$.firstOrderDiscount").value(0))   // pas de -10 % auto en vente directe
                    .andExpect(jsonPath("$.discount").value(1000))
                    .andExpect(jsonPath("$.deliveryFee").value(2000))
                    .andExpect(jsonPath("$.total").value(31000))
                    .andExpect(jsonPath("$.deliveryCity").value("Yaoundé"));

            assertThat(stockOf(robe)).isEqualTo(3);
            Optional<User> created = userRepository.findByPhone(phone);
            assertThat(created).isPresent();
            assertThat(created.get().getRoles()).extracting("name").containsExactly("CLIENT");

            // Aucun mot de passe ne permet de se connecter à ce compte
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\": \"%s\", \"password\": \"motdepasse-test\"}".formatted(phone)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Client déjà inscrit : la vente est rattachée à son compte (il la voit dans ses commandes)")
        void existingCustomerSeesTheSale() throws Exception {
            String phone = uniquePhone();
            String customer = registerCustomer(phone);
            Product sac = product("20000", 5);

            String body = confirm(directSaleJson(phone, "[{\"productId\": %d, \"quantity\": 1}]".formatted(sac.getId()),
                    "Douala", "Bonapriso", 1500, 0, 21500, "BANK_TRANSFER"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Number id = JsonPath.read(body, "$.orderId");

            mvc.perform(get("/api/orders/" + id).header("Authorization", bearer(customer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("Prix du jour : un produit en promo est compté au prix promo")
        void usesCurrentPromoPrice() throws Exception {
            Product p = product("10000", 5);
            promotion(p, new BigDecimal("20"));

            confirm(directSaleJson(uniquePhone(), "[{\"productId\": %d, \"quantity\": 2}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 0, 17500, "CASH"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.items[0].unitPrice").value(8000))
                    .andExpect(jsonPath("$.subtotal").value(16000))
                    .andExpect(jsonPath("$.total").value(17500));
        }

        @Test
        @DisplayName("Stock insuffisant : 409, aucun compte créé, stock inchangé")
        void insufficientStockCreatesNothing() throws Exception {
            String phone = uniquePhone();
            Product p = product("10000", 1);

            confirm(directSaleJson(phone, "[{\"productId\": %d, \"quantity\": 3}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 0, 31500, "CASH"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

            assertThat(stockOf(p)).isEqualTo(1);
            assertThat(userRepository.findByPhone(phone)).isEmpty();
        }

        @Test
        @DisplayName("Reçu envoyé au numéro WhatsApp saisi, livraison et réduction détaillées")
        void receiptForDirectSale() throws Exception {
            String phone = uniquePhone();
            Product p = product("15000", 5);

            String body = confirm(directSaleJson(phone, "[{\"productId\": %d, \"quantity\": 2}]".formatted(p.getId()),
                    "Yaoundé", "Bastos", 2000, 1000, 31000, "ORANGE_MONEY"))
                    .andReturn().getResponse().getContentAsString();

            URI link = URI.create(JsonPath.read(body, "$.receiptWhatsappUrl"));
            assertThat(link.getPath()).isEqualTo("/" + phone.substring(1));
            assertThat(whatsappText(link))
                    .contains("Réduction : -1 000 FCFA")
                    .contains("Livraison (Yaoundé) : 2 000 FCFA")
                    .contains("Montant payé : 31 000 FCFA")
                    .contains("Orange Money");
        }
    }

    // =============================================================================================
    @Nested
    @DisplayName("Tarif de livraison proposé dans le formulaire")
    class DeliveryFeeQuote {

        @Test
        @DisplayName("Yaoundé sous 50 000 : 2 000 ; à partir de 50 000 : 0 ; autre ville : 3 500")
        void quotesFee() throws Exception {
            quote("yaounde", 10000).andExpect(jsonPath("$.deliveryFee").value(2000));
            quote("Yaoundé", 50000).andExpect(jsonPath("$.deliveryFee").value(0));
            quote("Bafoussam", 10000).andExpect(jsonPath("$.deliveryFee").value(3500));
        }

        private ResultActions quote(String city, int amount) throws Exception {
            return mvc.perform(get("/api/admin/payments/delivery-fee")
                            .param("city", city)
                            .param("amount", String.valueOf(amount))
                            .header("Authorization", bearer(admin)))
                    .andExpect(status().isOk());
        }
    }

    // =============================================================================================
    @Nested
    @DisplayName("Requêtes refusées")
    class Rejected {

        @Test
        @DisplayName("N° de commande inconnu : 404")
        void unknownOrder() throws Exception {
            mvc.perform(get("/api/admin/payments/orders/999999999").header("Authorization", bearer(admin)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Aucun produit : 400 VALIDATION_ERROR")
        void noItems() throws Exception {
            confirm(directSaleJson(uniquePhone(), "[]", "Douala", "Akwa", 1500, 0, 1500, "CASH"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("WhatsApp du client invalide : 400 VALIDATION_ERROR")
        void invalidWhatsapp() throws Exception {
            Product p = product("5000", 5);
            confirm(directSaleJson("699112233", "[{\"productId\": %d, \"quantity\": 1}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 0, 6500, "CASH"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Mode de paiement absent : 400")
        void missingPaymentMethod() throws Exception {
            Product p = product("5000", 5);
            confirm(directSaleJson(uniquePhone(), "[{\"productId\": %d, \"quantity\": 1}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 0, 6500, null))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Réduction supérieure au sous-total : 400 BAD_REQUEST")
        void discountAboveSubtotal() throws Exception {
            Product p = product("5000", 5);
            confirm(directSaleJson(uniquePhone(), "[{\"productId\": %d, \"quantity\": 1}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 6000, 0, "CASH"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Compte client (sans rôle ADMIN) : 403 ; non connecté : 401")
        void accessRules() throws Exception {
            Product p = product("5000", 5);
            String json = directSaleJson(uniquePhone(), "[{\"productId\": %d, \"quantity\": 1}]".formatted(p.getId()),
                    "Douala", "Akwa", 1500, 0, 6500, "CASH");

            mvc.perform(post("/api/admin/payments").header("Authorization", bearer(registerCustomer()))
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/payments").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isUnauthorized());
        }
    }

    // --- Outils -----------------------------------------------------------------------------------

    private ResultActions confirm(String json) throws Exception {
        return mvc.perform(post("/api/admin/payments")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Confirmation d'une commande du site : WhatsApp et livraison repris de la commande. */
    private static String siteOrderJson(long orderId, String items, int deliveryFee, int discount, int amountPaid,
                                        String method) {
        return """
                {"orderId": %d, "customerWhatsapp": "+237699112233", "items": %s,
                 "deliveryCity": "Douala", "deliveryDistrict": "Akwa", "deliveryFee": %d,
                 "discount": %d, "amountPaid": %d, "paymentMethod": %s, "paidOn": "%s"}
                """.formatted(orderId, items, deliveryFee, discount, amountPaid, quoted(method), PAID_ON);
    }

    private static String directSaleJson(String whatsapp, String items, String city, String district,
                                         int deliveryFee, int discount, int amountPaid, String method) {
        return """
                {"customerWhatsapp": "%s", "items": %s,
                 "deliveryCity": "%s", "deliveryDistrict": "%s", "deliveryFee": %d,
                 "discount": %d, "amountPaid": %d, "paymentMethod": %s, "paidOn": "%s"}
                """.formatted(whatsapp, items, city, district, deliveryFee, discount, amountPaid, quoted(method), PAID_ON);
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getStockQuantity();
    }

    private static String whatsappText(URI link) {
        String query = link.getRawQuery();
        assertThat(query).startsWith("text=");
        return URLDecoder.decode(query.substring("text=".length()), StandardCharsets.UTF_8);
    }
}
