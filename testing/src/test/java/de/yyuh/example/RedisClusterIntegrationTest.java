package de.yyuh.example;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import de.yyuh.celery.Celery;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.platform.redis.cache.cluster.CeleryRedisCacheClusterPlatform;
import de.yyuh.celery.platform.redis.cluster.CeleryRedisClusterPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Celery Redis Cluster messaging and cache platforms.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisClusterIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS_CLUSTER = new GenericContainer<>(
      DockerImageName.parse("grokzen/redis-cluster:7.0.15"))
      .withExposedPorts(7000, 7001, 7002, 7003, 7004, 7005)
      .waitingFor(Wait.forLogMessage(".*Cluster state changed.*ok.*", 1))
      .withStartupTimeout(Duration.ofSeconds(60));

  static Celery celery;

  @BeforeAll
  static void setUp() {
    System.setProperty("REDIS_USER", "default");
    System.setProperty("REDIS_PASSWORD", "");
    System.setProperty("REDIS_HOST", REDIS_CLUSTER.getHost());
    System.setProperty("REDIS_PORT", String.valueOf(REDIS_CLUSTER.getMappedPort(7000)));

    celery = Celery.builder()
        .withId("test-service-redis-cluster")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryRedisClusterPlatform.class)
        .registerPlatform(CeleryRedisCacheClusterPlatform.class)
        .build();
  }

  @Test
  @Order(1)
  @DisplayName("Redis Cluster Messaging → connect and check connection")
  void messagingConnect() {
    final IMessagingProvider provider = celery.getPlatformById("redis-cluster").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();

    assertThat(provider.isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("Redis Cluster Messaging → publish message")
  void messagingPublish() {
    final IMessagingProvider provider = celery.getPlatformById("redis-cluster").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();

    final StringValue msg = StringValue.of("hello-redis-cluster");
    final Message packed = Any.pack(msg, "type.googleapis.com/google.protobuf.StringValue");

    provider.publish("cluster.msg.test", packed).join();
  }

  @Test
  @Order(3)
  @DisplayName("Redis Cluster Messaging → subscribe and unsubscribe")
  void messagingSubscribeUnsubscribe() {
    final IMessagingProvider provider = celery.getPlatformById("redis-cluster").orElseThrow()
        .provider(IMessagingProvider.class).orElseThrow();

    provider.subscribe("cluster.msg.sub").join();
    provider.unsubscribe("cluster.msg.sub").join();
  }

  @Test
  @Order(4)
  @DisplayName("Redis Cluster Cache → connect and check connection")
  void cacheConnect() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    assertThat(cache.isConnected().join()).isTrue();
  }

  @Test
  @Order(5)
  @DisplayName("Redis Cluster Cache → set, get, exists, delete")
  void cacheOperations() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    final String key = "cluster:test:key";
    final byte[] value = "Hello, Redis Cluster!".getBytes(StandardCharsets.UTF_8);

    cache.set(key, value, Duration.ofMinutes(1)).join();
    assertThat(cache.exists(key).join()).isTrue();

    final Optional<byte[]> retrieved = cache.get(key).join();
    assertThat(retrieved).isPresent();
    assertThat(new String(retrieved.get(), StandardCharsets.UTF_8)).isEqualTo("Hello, Redis Cluster!");

    cache.delete(key).join();
    assertThat(cache.exists(key).join()).isFalse();
  }

  @Test
  @Order(6)
  @DisplayName("Redis Cluster Cache → get missing key returns empty")
  void cacheMiss() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    final Optional<byte[]> result = cache.get("cluster:nonexistent").join();
    assertThat(result).isEmpty();
  }
}
