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
    // Use localhost directly: DockerComposeContainer.getServiceHost() can
    // return internal compose network IPs in Docker-in-Docker setups (e.g.
    // Kubernetes Jenkins agents) which are unreachable from the test JVM.
    // All compose services are bound to the Docker host and accessible via
    // localhost on the mapped ports.
    System.setProperty("MONGODB_HOST", "localhost");
    System.setProperty("MONGODB_PORT", String.valueOf(COMPOSE.getServicePort("mongodb", 27017)));

    System.setProperty("REDIS_USER", "default");
    System.setProperty("REDIS_PASSWORD", "testpass");
    System.setProperty("REDIS_HOST", "localhost");
    System.setProperty("REDIS_PORT", String.valueOf(COMPOSE.getServicePort("redis", 6379)));

    System.setProperty("NATS_USER", "nats");
    System.setProperty("NATS_PASSWORD", "nats-token");
    System.setProperty("NATS_HOST", "localhost");
    System.setProperty("NATS_PORT", String.valueOf(COMPOSE.getServicePort("nats", 4222)));

    System.setProperty("INFLUXDB_USER", "admin");
    System.setProperty("INFLUXDB_PASSWORD", "my-super-secret-token");
    System.setProperty("INFLUXDB_HOST", "localhost");
    System.setProperty("INFLUXDB_PORT", String.valueOf(COMPOSE.getServicePort("influxdb", 8086)));

    started = true;
  }
}
