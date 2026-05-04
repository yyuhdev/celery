package de.yyuh.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.BytesValue;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.platform.nats.provider.NatsMessagingProvider;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class NatsIntegrationTest {

  @Container
  private static final GenericContainer<?> NATS = new GenericContainer<>(
      DockerImageName.parse("nats:2-alpine"))
      .withCommand("nats-server", "--auth", "nats-token")
      .withExposedPorts(4222);

  private NatsMessagingProvider provider;

  @BeforeEach
  void setUp() {
    provider = new NatsMessagingProvider();
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
        com.google.protobuf.ByteString.copyFromUtf8("nats-test-message"));

    provider.publish("nats-test-channel", message)
        .get(10, TimeUnit.SECONDS);
  }

  @Test
  void shouldSubscribe() throws Exception {
    connect();

    provider.subscribe("nats-sub-channel")
        .get(10, TimeUnit.SECONDS);

    provider.unsubscribe("nats-sub-channel")
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
        "nats-token",
        NATS.getHost(),
        NATS.getMappedPort(4222),
        null, null, null, null);

    final var result = provider.connect(credentials)
        .get(10, TimeUnit.SECONDS);
    assertThat(result.isOk()).isTrue();
  }
}
