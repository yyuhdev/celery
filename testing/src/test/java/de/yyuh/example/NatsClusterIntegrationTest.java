package de.yyuh.example;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import de.yyuh.celery.Celery;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.platform.nats.cluster.CeleryNatsClusterPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsClusterIntegrationTest {

  @Container
  static final GenericContainer<?> NATS = new GenericContainer<>(
      DockerImageName.parse("nats:2-alpine"))
      .withExposedPorts(4222)
      .withCommand("nats-server", "--auth", "nats-cluster-token");

  static Celery celery;

  @BeforeAll
  static void setUp() {
    final String host = NATS.getHost();
    final int port = NATS.getMappedPort(4222);

    System.setProperty("NATS_USER", "nats");
    System.setProperty("NATS_PASSWORD", "nats-cluster-token");
    System.setProperty("NATS_HOST", host + ":" + port);
    System.setProperty("NATS_PORT", String.valueOf(port));

    celery = Celery.builder()
        .withId("test-service-nats-cluster")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryNatsClusterPlatform.class)
        .build();
  }

  private static IMessagingProvider getProvider() {
    return celery.getPlatformById("nats-cluster").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();
  }

  @Test
  @Order(1)
  @DisplayName("NATS Cluster → connect and check connection")
  void connectAndPing() {
    assertThat(getProvider().isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("NATS Cluster → publish message")
  void publishMessage() {
    final StringValue msg = StringValue.of("hello-cluster");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");

    getProvider().publish("cluster.test.publish", packed).join();
  }

  @Test
  @Order(3)
  @DisplayName("NATS Cluster → subscribe and unsubscribe")
  void subscribeAndUnsubscribe() {
    final IMessagingProvider provider = getProvider();

    provider.subscribe("cluster.test.channel").join();
    provider.unsubscribe("cluster.test.channel").join();
  }
}
