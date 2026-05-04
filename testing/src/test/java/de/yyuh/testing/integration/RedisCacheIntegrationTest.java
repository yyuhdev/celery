package de.yyuh.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.platform.redis.cache.RedisCacheProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class RedisCacheIntegrationTest {

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("docker.dragonflydb.io/dragonflydb/dragonfly"))
      .withCommand("--requirepass=testpass")
      .withExposedPorts(6379);

  private RedisCacheProvider provider;

  @BeforeEach
  void setUp() {
    provider = new RedisCacheProvider();
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
  void shouldSetAndGet() throws Exception {
    connect();

    final var key = "test-key";
    final var value = "hello-world".getBytes(StandardCharsets.UTF_8);

    provider.set(key, value, Duration.ofSeconds(60))
        .get(10, TimeUnit.SECONDS);

    final var result = provider.get(key)
        .get(10, TimeUnit.SECONDS);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  void shouldReturnEmptyForMissingKey() throws Exception {
    connect();

    final var result = provider.get("missing-key")
        .get(10, TimeUnit.SECONDS);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldDeleteKey() throws Exception {
    connect();

    final var key = "del-key";
    provider.set(key, "data".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60))
        .get(10, TimeUnit.SECONDS);

    provider.delete(key).get(10, TimeUnit.SECONDS);

    final var result = provider.get(key)
        .get(10, TimeUnit.SECONDS);
    assertThat(result).isEmpty();
  }

  @Test
  void shouldCheckExists() throws Exception {
    connect();

    final var key = "exists-key";
    provider.set(key, "data".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(60))
        .get(10, TimeUnit.SECONDS);

    final var exists = provider.exists(key)
        .get(10, TimeUnit.SECONDS);
    assertThat(exists).isTrue();

    final var missing = provider.exists("nonexistent")
        .get(10, TimeUnit.SECONDS);
    assertThat(missing).isFalse();
  }

  @Test
  void isConnectedReturnsFalseBeforeConnect() throws Exception {
    final var connected = provider.isConnected()
        .get(10, TimeUnit.SECONDS);
    assertThat(connected).isFalse();
  }

  private void connect() throws Exception {
    final var credentials = new Credentials(
        null,
        "testpass",
        REDIS.getHost(),
        REDIS.getMappedPort(6379),
        null, null, null, null);

    final var result = provider.connect(credentials)
        .get(10, TimeUnit.SECONDS);
    assertThat(result.isOk()).isTrue();
  }
}
