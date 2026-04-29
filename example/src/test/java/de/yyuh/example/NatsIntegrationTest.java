package de.yyuh.example;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import de.yyuh.celery.Celery;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.platform.nats.CeleryNatsPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsIntegrationTest {

  @Container
  static final GenericContainer<?> NATS = new GenericContainer<>(
      DockerImageName.parse("nats:2-alpine"))
      .withExposedPorts(4222)
      .withCommand("nats-server", "--auth", "nats-token");

  static Celery celery;

  @BeforeAll
  static void setUp() {
    System.setProperty("NATS_USER", "nats");
    System.setProperty("NATS_PASSWORD", "nats-token");
    System.setProperty("NATS_HOST", NATS.getHost());
    System.setProperty("NATS_PORT", String.valueOf(NATS.getMappedPort(4222)));

    celery = Celery.builder()
        .withId("test-service-nats")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryNatsPlatform.class)
        .build();
  }

  private static IMessagingProvider getProvider() {
    return celery.getPlatformById("nats").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();
  }

  @Test
  @Order(1)
  @DisplayName("NATS → connect and check connection")
  void connectAndPing() {
    assertThat(getProvider().isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("NATS → publish message")
  void publishMessage() {
    final StringValue msg = StringValue.of("hello-nats");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");

    getProvider().publish("test.publish", packed).join();
  }

  @Test
  @Order(3)
  @DisplayName("NATS → subscribe and receive message")
  void subscribeAndReceive() throws Exception {
    final IMessagingProvider provider = getProvider();

    provider.subscribe("test.roundtrip").join();

    final StringValue msg = StringValue.of("roundtrip-nats");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");
    provider.publish("test.roundtrip", packed).join();

    Thread.sleep(500);

    provider.unsubscribe("test.roundtrip").join();
  }

  @Test
  @Order(4)
  @DisplayName("NATS → unsubscribe")
  void unsubscribeChannel() {
    final IMessagingProvider provider = getProvider();

    provider.subscribe("test.unsub").join();
    provider.unsubscribe("test.unsub").join();
  }
}
