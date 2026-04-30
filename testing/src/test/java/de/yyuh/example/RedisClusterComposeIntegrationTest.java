package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.platform.redis.cache.cluster.CeleryRedisCacheClusterPlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisClusterComposeIntegrationTest {

  private static final File COMPOSE_FILE = new File(
      "src/test/resources/docker-compose-redis-cluster.yml");

  @Container
  static final DockerComposeContainer<?> REDIS_CLUSTER = new DockerComposeContainer<>(COMPOSE_FILE)
      .withExposedService("redis1", 6379)
      .withExposedService("redis2", 6379)
      .withExposedService("redis3", 6379)
      .withExposedService("redis4", 6379)
      .withExposedService("redis5", 6379)
      .withExposedService("redis6", 6379);

  static Celery celery;

  @BeforeAll
  static void setUp() throws Exception {
    final String host = REDIS_CLUSTER.getServiceHost("redis1", 6379);
    final int port = REDIS_CLUSTER.getServicePort("redis1", 6379);

    System.setProperty("REDIS_USER", "default");
    System.setProperty("REDIS_PASSWORD", "");
    System.setProperty("REDIS_HOST", host);
    System.setProperty("REDIS_PORT", String.valueOf(port));

    for (int i = 0; i < 30; i++) {
      try (final var client = io.lettuce.core.cluster.RedisClusterClient.create(
          io.lettuce.core.RedisURI.create(host, port))) {
        final var conn = client.connect();
        final String info = conn.sync().clusterInfo();
        if (info.contains("cluster_state:ok")) {
          conn.close();
          client.shutdown();
          break;
        }
        conn.close();
      } catch (Exception e) {
      }
      Thread.sleep(2000);
    }

    celery = Celery.builder()
        .withId("test-service-redis-compose")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryRedisCacheClusterPlatform.class)
        .build();
  }

  @Test
  @Order(1)
  @DisplayName("Redis Compose Cluster Cache → connect and check connection")
  void cacheConnect() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    assertThat(cache.isConnected().join()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("Redis Compose Cluster Cache → set, get, exists, delete")
  void cacheOperations() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    final String key = "compose:cluster:key";
    final byte[] value = "Hello, Compose Cluster!".getBytes(StandardCharsets.UTF_8);

    cache.set(key, value, Duration.ofMinutes(1)).join();
    assertThat(cache.exists(key).join()).isTrue();

    final Optional<byte[]> retrieved = cache.get(key).join();
    assertThat(retrieved).isPresent();
    assertThat(new String(retrieved.get(), StandardCharsets.UTF_8)).isEqualTo("Hello, Compose Cluster!");

    cache.delete(key).join();
    assertThat(cache.exists(key).join()).isFalse();
  }

  @Test
  @Order(3)
  @DisplayName("Redis Compose Cluster Cache → get missing key returns empty")
  void cacheMiss() {
    final ICacheProvider cache = celery.getPlatformById("redis-cache-cluster").orElseThrow()
        .provider(ICacheProvider.class).orElseThrow();

    final Optional<byte[]> result = cache.get("compose:cluster:nonexistent").join();
    assertThat(result).isEmpty();
  }
}
