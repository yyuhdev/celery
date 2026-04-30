package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.ILogEntry;
import de.yyuh.celery.api.provider.ITimeseriesProvider;
import de.yyuh.celery.logging.CeleryInfluxDBPlatform;
import org.junit.jupiter.api.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InfluxDBIntegrationTest extends BaseCeleryIntegrationTest {

  static Celery celery;

  @BeforeAll
  static void setUp() {
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
