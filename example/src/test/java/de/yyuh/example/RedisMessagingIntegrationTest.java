package de.yyuh.example;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import de.yyuh.celery.Celery;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.platform.redis.CeleryRedisPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisMessagingIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("docker.dragonflydb.io/dragonflydb/dragonfly"))
      .withExposedPorts(6379)
      .withCommand("--requirepass=redis-token");

  static Celery celery;

  @BeforeAll
  static void setUp() {
    System.setProperty("REDIS_USER", "default");
    System.setProperty("REDIS_PASSWORD", "redis-token");
    System.setProperty("REDIS_HOST", REDIS.getHost());
    System.setProperty("REDIS_PORT", String.valueOf(REDIS.getMappedPort(6379)));

    celery = Celery.builder()
        .withId("test-service-redis-msg")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryRedisPlatform.class)
        .build();
  }

  private static IMessagingProvider getProvider() {
    return celery.getPlatformById("redis").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();
  }

  @Test
  @Order(1)
  @DisplayName("Redis Messaging → connect and check connection")
  void connectAndPing() {
    assertThat(getProvider().isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("Redis Messaging → publish message")
  void publishMessage() {
    final StringValue msg = StringValue.of("hello-redis");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");

    getProvider().publish("redis.test.publish", packed).join();
  }

  @Test
  @Order(3)
  @DisplayName("Redis Messaging → subscribe and unsubscribe")
  void subscribeAndUnsubscribe() {
    final IMessagingProvider provider = getProvider();

    provider.subscribe("redis.test.channel").join();
    provider.unsubscribe("redis.test.channel").join();
  }
}
