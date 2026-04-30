package de.yyuh.example;

import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for Celery integration tests.
 *
 * <p>
 * Delegates to {@link CeleryTestContainers} which starts a shared Docker
 * Compose stack (MongoDB, Dragonfly, NATS, InfluxDB) once across all
 * subclasses and injects system properties for credential resolution.
 */
public abstract class BaseCeleryIntegrationTest {

  @BeforeAll
  static void setUpContainers() {
    CeleryTestContainers.startOnce();
  }
}
