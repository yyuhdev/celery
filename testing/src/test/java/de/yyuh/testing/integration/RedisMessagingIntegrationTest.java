package de.yyuh.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.BytesValue;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.platform.redis.provider.RedisMessagingProvider;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class RedisMessagingIntegrationTest {

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("docker.dragonflydb.io/dragonflydb/dragonfly"))
      .withCommand("--requirepass=testpass")
      .withExposedPorts(6379);

  private RedisMessagingProvider provider;

  @BeforeEach
  void setUp() {
    provider = new RedisMessagingProvider();
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
  void shouldPublishMessage() throws Exception {
    connect();

    final var message = BytesValue.of(
        com.google.protobuf.ByteString.copyFromUtf8("test-message"));

    provider.publish("test-channel", message)
        .get(10, TimeUnit.SECONDS);
  }

  @Test
  void shouldSubscribe() throws Exception {
    connect();

    provider.subscribe("subscription-channel")
        .get(10, TimeUnit.SECONDS);

    provider.unsubscribe("subscription-channel")
        .get(10, TimeUnit.SECONDS);
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
