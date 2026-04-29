package de.yyuh.example;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import de.yyuh.celery.Celery;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.platform.nats.cluster.CeleryNatsClusterPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NatsClusterComposeIntegrationTest {

  private static final File COMPOSE_FILE = new File(
      "src/test/resources/docker-compose-nats-cluster.yml");

  @Container
  static final DockerComposeContainer<?> NATS_CLUSTER = new DockerComposeContainer<>(COMPOSE_FILE)
      .withExposedService("nats1", 4222)
      .withExposedService("nats2", 4222)
      .withExposedService("nats3", 4222);

  static Celery celery;

  @BeforeAll
  static void setUp() {
    final String host1 = NATS_CLUSTER.getServiceHost("nats1", 4222);
    final int port1 = NATS_CLUSTER.getServicePort("nats1", 4222);
    final String host2 = NATS_CLUSTER.getServiceHost("nats2", 4222);
    final int port2 = NATS_CLUSTER.getServicePort("nats2", 4222);
    final String host3 = NATS_CLUSTER.getServiceHost("nats3", 4222);
    final int port3 = NATS_CLUSTER.getServicePort("nats3", 4222);

    final String clusterHosts = String.format("%s:%d,%s:%d,%s:%d",
        host1, port1, host2, port2, host3, port3);

    System.setProperty("NATS_USER", "nats");
    System.setProperty("NATS_PASSWORD", "compose-token");
    System.setProperty("NATS_HOST", clusterHosts);
    System.setProperty("NATS_PORT", String.valueOf(port1));

    celery = Celery.builder()
        .withId("test-service-nats-compose")
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
  @DisplayName("NATS Compose Cluster → connect and check connection")
  void connectAndPing() {
    assertThat(getProvider().isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("NATS Compose Cluster → publish message")
  void publishMessage() {
    final StringValue msg = StringValue.of("hello-nats-compose");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");

    getProvider().publish("compose.nats.test", packed).join();
  }

  @Test
  @Order(3)
  @DisplayName("NATS Compose Cluster → subscribe and unsubscribe")
  void subscribeAndUnsubscribe() {
    final IMessagingProvider provider = getProvider();

    provider.subscribe("compose.nats.sub").join();
    provider.unsubscribe("compose.nats.sub").join();
  }
}
