package de.yyuh.example;

import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

/**
 * Singleton holder for the shared Docker Compose test stack.
 *
 * <p>
 * The compose stack (MongoDB, Dragonfly, NATS, InfluxDB) is started once
 * and system properties are injected. All integration tests that extend
 * {@link BaseCeleryIntegrationTest} share this single container instance.
 */
final class CeleryTestContainers {

  private static final File COMPOSE_FILE = new File(
      "src/test/resources/docker-compose-test.yml");

  static final DockerComposeContainer<?> COMPOSE = new DockerComposeContainer<>(COMPOSE_FILE)
      .withExposedService("mongodb", 27017)
      .withExposedService("redis", 6379)
      .withExposedService("nats", 4222)
      .withExposedService("influxdb", 8086);

  private static volatile boolean started;

  private CeleryTestContainers() {
  }

  static synchronized void startOnce() {
    if (started) {
      return;
    }
    COMPOSE.start();

    System.setProperty("MONGODB_USER", "root");
    System.setProperty("MONGODB_PASSWORD", "root");
    System.setProperty("MONGODB_HOST", COMPOSE.getServiceHost("mongodb", 27017));
    System.setProperty("MONGODB_PORT", String.valueOf(COMPOSE.getServicePort("mongodb", 27017)));

    System.setProperty("REDIS_USER", "default");
    System.setProperty("REDIS_PASSWORD", "testpass");
    System.setProperty("REDIS_HOST", COMPOSE.getServiceHost("redis", 6379));
    System.setProperty("REDIS_PORT", String.valueOf(COMPOSE.getServicePort("redis", 6379)));

    System.setProperty("NATS_USER", "nats");
    System.setProperty("NATS_PASSWORD", "nats-token");
    System.setProperty("NATS_HOST", COMPOSE.getServiceHost("nats", 4222));
    System.setProperty("NATS_PORT", String.valueOf(COMPOSE.getServicePort("nats", 4222)));

    System.setProperty("INFLUXDB_USER", "admin");
    System.setProperty("INFLUXDB_PASSWORD", "my-super-secret-token");
    System.setProperty("INFLUXDB_HOST", COMPOSE.getServiceHost("influxdb", 8086));
    System.setProperty("INFLUXDB_PORT", String.valueOf(COMPOSE.getServicePort("influxdb", 8086)));

    started = true;
  }
}
