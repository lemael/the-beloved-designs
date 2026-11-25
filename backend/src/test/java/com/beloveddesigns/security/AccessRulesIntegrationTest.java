package com.beloveddesigns.security;

import com.beloveddesigns.support.AbstractApiIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle d'accès : non connecté → 401 UNAUTHORIZED (le frontend redirige vers la connexion),
 * connecté sans le rôle requis → 403, pages publiques accessibles sans jeton.
 */
class AccessRulesIntegrationTest extends AbstractApiIntegrationTest {

    @Test
    @DisplayName("Route client sans jeton (GET /api/cart) : 401 UNAUTHORIZED")
    void anonymousOnCustomerRouteGets401() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Back-office sans jeton : 401 UNAUTHORIZED")
    void anonymousOnAdminRouteGets401() throws Exception {
        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Back-office avec un compte client (sans rôle ADMIN) : 403")
    void customerOnAdminRouteGets403() throws Exception {
        String token = registerCustomer();

        mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Jeton invalide : 401 TOKEN_INVALID (inchangé)")
    void invalidTokenGets401() throws Exception {
        mvc.perform(get("/api/cart").header("Authorization", bearer("pas-un-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("Catalogue public sans jeton : 200")
    void publicCatalogIsOpen() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }
}
