package com.beloveddesigns.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base des tests d'intégration : démarre un vrai PostgreSQL jetable dans Docker (Testcontainers),
 * sur lequel Flyway applique les migrations comme en production.
 *
 * <p>Le conteneur est démarré une seule fois pour toute la JVM et partagé par toutes les classes
 * de test (démarrage ~ quelques secondes au lieu d'un par classe). Ryuk, le nettoyeur de
 * Testcontainers, le supprime à la fin. Chaque test doit donc créer ses propres données et ne pas
 * dépendre de l'ordre d'exécution.
 *
 * <p>Prérequis : Docker doit tourner sur la machine (en local comme sur GitHub Actions).
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    /** Même version majeure que docker-compose.yml. Jamais fermé volontairement : partagé, Ryuk le supprime. */
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("beloved_designs")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        // Les entités n'indiquent pas de schéma : on garde currentSchema dans l'URL, comme en prod.
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&currentSchema=beloved_designs");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Numéro WhatsApp fictif de la boutique (en prod : variable ADMIN_WHATSAPP sur Railway).
        registry.add("app.orders.admin-whatsapp", () -> ADMIN_WHATSAPP);
    }

    /** Numéro WhatsApp de la boutique utilisé par les tests de commande. */
    protected static final String ADMIN_WHATSAPP = "+237600000001";
}
