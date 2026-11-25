package com.beloveddesigns.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests qui appellent l'API HTTP (MockMvc : filtres de sécurité, validation, gestion des
 * erreurs compris), avec de quoi créer un client et obtenir son jeton JWT.
 */
@AutoConfigureMockMvc
public abstract class AbstractApiIntegrationTest extends AbstractIntegrationTest {

    /** Suffixe unique (numéros, slugs) : la base est partagée par tous les tests. */
    protected static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Autowired
    protected MockMvc mvc;

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
}
