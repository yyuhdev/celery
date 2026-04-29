package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.celery.platform.redis.cache.CeleryRedisCachePlatform;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CeleryIntegrationTest {

  @Container
  static final GenericContainer<?> MONGO = new GenericContainer<>(
      DockerImageName.parse("mongo:7.0"))
      .withExposedPorts(27017)
      .withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
      .withEnv("MONGO_INITDB_ROOT_PASSWORD", "root");

  @Container
  static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("docker.dragonflydb.io/dragonflydb/dragonfly"))
      .withExposedPorts(6379)
      .withCommand("--requirepass=testpass");

  static Celery celery;

  @BeforeAll
  static void setUp() {
    System.setProperty("MONGODB_USER", MONGO.getEnvMap().getOrDefault("MONGO_INITDB_ROOT_USERNAME", "root"));
    System.setProperty("MONGODB_PASSWORD", MONGO.getEnvMap().getOrDefault("MONGO_INITDB_ROOT_PASSWORD", "root"));
    System.setProperty("MONGODB_HOST", MONGO.getHost());
    System.setProperty("MONGODB_PORT", String.valueOf(MONGO.getMappedPort(27017)));

    System.setProperty("REDIS_PASSWORD", "testpass");
    System.setProperty("REDIS_HOST", REDIS.getHost());
    System.setProperty("REDIS_PORT", String.valueOf(REDIS.getMappedPort(6379)));

    celery = Celery.builder()
        .withId("test-service")
        .registerCredentialProvider(new SystemPropertyCredentialProvider())
        .registerPlatform(CeleryMongoDBPlatform.class)
        .registerPlatform(CeleryRedisCachePlatform.class)
        .build();
  }

  @Test
  @Order(1)
  @DisplayName("MongoDB → save and find entity by _id")
  void saveAndFindUser() {
    final var platform = celery.getPlatformById("mongodb").orElseThrow();

    @SuppressWarnings("unchecked")
    final IDatabaseProvider<User, IQuery<User>> db = (IDatabaseProvider<User, IQuery<User>>) platform.defaultProvider();

    assertThat(db.isConnected().join()).isTrue();

    final User user = new User("TestUser", "test@example.com");
    db.save(user).join();

    final IQuery<User> byId = queryById(User.class, user.id());
    final List<User> found = db.find(byId).join();

    assertThat(found).hasSize(1);
    assertThat(found.get(0).name()).isEqualTo("TestUser");
    assertThat(found.get(0).email()).isEqualTo("test@example.com");

    db.delete(byId).join();
  }

  @Test
  @Order(2)
  @DisplayName("MongoDB → find all returns saved entities")
  void findAllUsers() {
    final var platform = celery.getPlatformById("mongodb").orElseThrow();

    @SuppressWarnings("unchecked")
    final IDatabaseProvider<User, IQuery<User>> db = (IDatabaseProvider<User, IQuery<User>>) platform.defaultProvider();

    final User u1 = new User("Alpha", "alpha@example.com");
    final User u2 = new User("Beta", "beta@example.com");
    db.save(u1).join();
    db.save(u2).join();

    final IQuery<User> matchAll = new IQuery<>() {
      @Override
      public Class<User> entityClass() {
        return User.class;
      }

      @Override
      public Map<String, Object> filters() {
        return Map.of();
      }

      @Override
      public Optional<Integer> limit() {
        return Optional.empty();
      }

      @Override
      public Optional<Integer> offset() {
        return Optional.empty();
      }
    };
    final List<User> all = db.find(matchAll).join();
    assertThat(all).hasSizeGreaterThanOrEqualTo(2);

    db.delete(queryById(User.class, u1.id())).join();
    db.delete(queryById(User.class, u2.id())).join();
  }

  @Test
  @Order(3)
  @DisplayName("Redis Cache → set, get, exists, delete")
  void redisCacheOperations() {
    final var platform = celery.getPlatformById("redis-cache").orElseThrow();

    final ICacheProvider cache = platform.provider(ICacheProvider.class).orElseThrow();
    assertThat(cache.isConnected().join()).isTrue();

    final String key = "test:key";
    final byte[] value = "Hello, Redis!".getBytes(StandardCharsets.UTF_8);

    cache.set(key, value, Duration.ofMinutes(1)).join();
    assertThat(cache.exists(key).join()).isTrue();

    final Optional<byte[]> retrieved = cache.get(key).join();
    assertThat(retrieved).isPresent();
    assertThat(new String(retrieved.get(), StandardCharsets.UTF_8)).isEqualTo("Hello, Redis!");

    cache.delete(key).join();
    assertThat(cache.exists(key).join()).isFalse();
  }

  @Test
  @Order(4)
  @DisplayName("Redis Cache → get missing key returns empty")
  void redisCacheMiss() {
    final var platform = celery.getPlatformById("redis-cache").orElseThrow();

    final ICacheProvider cache = platform.provider(ICacheProvider.class).orElseThrow();

    final Optional<byte[]> result = cache.get("nonexistent:key").join();
    assertThat(result).isEmpty();
  }

  private static <T> IQuery<T> queryById(final Class<T> entityClass, final String id) {
    return new IQuery<>() {
      @Override
      public Class<T> entityClass() {
        return entityClass;
      }

      @Override
      public Map<String, Object> filters() {
        return Map.of("_id", id);
      }

      @Override
      public Optional<Integer> limit() {
        return Optional.of(1);
      }

      @Override
      public Optional<Integer> offset() {
        return Optional.empty();
      }
    };
  }
}
