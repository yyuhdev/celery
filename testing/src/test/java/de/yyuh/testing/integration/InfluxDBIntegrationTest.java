package de.yyuh.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.logging.provider.InfluxDBTimeseriesProvider;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class InfluxDBIntegrationTest {

  @Container
  private static final GenericContainer<?> INFLUXDB = new GenericContainer<>(
      DockerImageName.parse("influxdb:2.7"))
      .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
      .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
      .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "admin12345")
      .withEnv("DOCKER_INFLUXDB_INIT_ORG", "celery")
      .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", "logging")
      .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", "my-super-secret-token")
      .withExposedPorts(8086);

  private InfluxDBTimeseriesProvider provider;

  @BeforeEach
  void setUp() {
    provider = new InfluxDBTimeseriesProvider();
  }

  @AfterEach
  void tearDown() {
    provider.close();
  }

  @Test
  void shouldConnect() throws Exception {
    connect();

    final var connected = provider.isConnected()
        .get(10, TimeUnit.SECONDS);
    assertThat(connected).isTrue();
  }

  @Test
  void shouldSaveLogEntry() throws Exception {
    connect();

    final var entry = new ILogEntry() {
      @Override
      public String toString() {
        return "test-log-entry";
      }
    };

    provider.save(entry).get(10, TimeUnit.SECONDS);
  }

  @Test
  void isConnectedReturnsFalseBeforeConnect() throws Exception {
    final var connected = provider.isConnected()
        .get(10, TimeUnit.SECONDS);
    assertThat(connected).isFalse();
  }

  private void connect() throws Exception {
    final var credentials = new Credentials(
        "celery",
        "my-super-secret-token",
        INFLUXDB.getHost(),
        INFLUXDB.getMappedPort(8086),
        "logging",
        null, null, null);

    final var result = provider.connect(credentials)
        .get(10, TimeUnit.SECONDS);
    assertThat(result.isOk()).isTrue();
  }
}
