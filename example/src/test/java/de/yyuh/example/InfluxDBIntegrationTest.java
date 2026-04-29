package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.provider.ITimeseriesProvider;
import de.yyuh.celery.logging.CeleryInfluxDBPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class InfluxDBIntegrationTest {

  private static final String ADMIN_TOKEN = "my-super-secret-token";

  @Container
  static final GenericContainer<?> INFLUXDB = new GenericContainer<>(
      DockerImageName.parse("influxdb:2.7"))
      .withExposedPorts(8086)
      .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
      .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
      .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "admin12345")
      .withEnv("DOCKER_INFLUXDB_INIT_ORG", "celery")
      .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", "logging")
      .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", ADMIN_TOKEN);

  static Celery celery;

  @BeforeAll
  static void setUp() {
    System.setProperty("INFLUXDB_USER", "admin");
    System.setProperty("INFLUXDB_PASSWORD", ADMIN_TOKEN);
    System.setProperty("INFLUXDB_HOST", INFLUXDB.getHost());
    System.setProperty("INFLUXDB_PORT", String.valueOf(INFLUXDB.getMappedPort(8086)));

    celery = Celery.builder()
        .withId("test-service-influx")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryInfluxDBPlatform.class)
        .build();
  }

  private static ITimeseriesProvider getProvider() {
    return celery.getPlatformById("influx").orElseThrow()
        .provider(ITimeseriesProvider.class).orElseThrow();
  }

  @Test
  @Order(1)
  @DisplayName("InfluxDB → connect and check connection")
  void connectAndPing() {
    assertThat(getProvider().isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("InfluxDB → save log entry")
  void saveLogEntry() {
    final ILogEntry entry = new ILogEntry() {
      @Override
      public String toString() {
        return "Test log message";
      }
    };
    getProvider().save(entry).join();
  }

  @Test
  @Order(3)
  @DisplayName("InfluxDB → delete by timestamp")
  void deleteByTimestamp() {
    getProvider().delete(Instant.now()).join();
  }
}
