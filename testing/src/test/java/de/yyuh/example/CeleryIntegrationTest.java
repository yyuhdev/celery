package de.yyuh.example;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IDatabaseProvider;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.celery.platform.redis.cache.CeleryRedisCachePlatform;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CeleryIntegrationTest extends BaseCeleryIntegrationTest {

  static Celery celery;

  @BeforeAll
  static void setUp() {
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
