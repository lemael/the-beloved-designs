package com.beloveddesigns;

import com.beloveddesigns.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Vérifie que l'application démarre entièrement : configuration Spring, migrations Flyway
 * appliquées sur une base vide, et entités JPA cohérentes avec le schéma (ddl-auto=validate).
 */
class BackendApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
