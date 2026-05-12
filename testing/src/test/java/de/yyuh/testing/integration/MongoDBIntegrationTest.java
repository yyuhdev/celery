package de.yyuh.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.yyuh.celery.Celery;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.query.AbstractQuery;
import de.yyuh.celery.api.query.IQuery;
import de.yyuh.celery.api.query.impl.IDQuery;
import de.yyuh.celery.platform.mongodb.CeleryMongoDBPlatform;
import de.yyuh.celery.platform.mongodb.provider.MongoDatabaseProvider;
import de.yyuh.testing.entity.TestEntity;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class MongoDBIntegrationTest {

  private static final String MONGO_USER = "root";
  private static final String MONGO_PASS = "root";

  @Container
  private static final GenericContainer<?> MONGO = new GenericContainer<>(
      DockerImageName.parse("mongo:7.0"))
      .withEnv("MONGO_INITDB_ROOT_USERNAME", MONGO_USER)
      .withEnv("MONGO_INITDB_ROOT_PASSWORD", MONGO_PASS)
      .withExposedPorts(27017);

  private MongoDatabaseProvider provider;

  @BeforeEach
  void setUp() {
    provider = new MongoDatabaseProvider();
  }

  @AfterEach
  void tearDown() {
    provider.close();
  }

  @Test
  void shouldConnect() throws Exception {
    final var credentials = buildCredentials();

    final var result = provider.connect(credentials)
        .get(10, TimeUnit.SECONDS);

    assertThat(result.isOk()).isTrue();
    assertThat(result.unwrap()).isGreaterThanOrEqualTo(0);

    final var connected = provider.isConnected()
        .get(10, TimeUnit.SECONDS);
    assertThat(connected).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSaveAndGetEntity() throws Exception {
    connect();

    final var entity = new TestEntity("test-1", "test-entity", 42);
    provider.save(entity).get(10, TimeUnit.SECONDS);

    final var query = (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(TestEntity.class, "test-1").build();
    final var found = provider.get(query)
        .get(10, TimeUnit.SECONDS)
        .orElseThrow();

    assertThat(found).isEqualTo(entity);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFindEntities() throws Exception {
    connect();

    final var entity1 = new TestEntity("find-1", "alpha", 10);
    final var entity2 = new TestEntity("find-2", "beta", 20);

    provider.save(entity1).get(10, TimeUnit.SECONDS);
    provider.save(entity2).get(10, TimeUnit.SECONDS);

    final var query = (IQuery<IEntity>) (IQuery<?>) EmptyQuery.of(TestEntity.class);
    final var results = provider.find(query)
        .get(10, TimeUnit.SECONDS);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results).extracting("id").contains("find-1", "find-2");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldDeleteEntity() throws Exception {
    connect();

    final var entity = new TestEntity("del-1", "to-delete", 99);
    provider.save(entity).get(10, TimeUnit.SECONDS);

    final var delQuery = (IQuery<IEntity>) (IQuery<?>) IDQuery.builder(TestEntity.class, "del-1").build();
    provider.delete(delQuery).get(10, TimeUnit.SECONDS);

    final var found = provider.get(delQuery)
        .get(10, TimeUnit.SECONDS);
    assertThat(found).isEmpty();
  }

  @Test
  void isConnectedReturnsFalseBeforeConnect() throws Exception {
    final var connected = provider.isConnected()
        .get(10, TimeUnit.SECONDS);
    assertThat(connected).isFalse();
  }

  private void connect() throws Exception {
    provider.connect(buildCredentials()).get(10, TimeUnit.SECONDS);
  }

  private Credentials buildCredentials() {
    return new Credentials(
        MONGO_USER,
        MONGO_PASS,
        MONGO.getHost(),
        "neow",
        MONGO.getMappedPort(27017),
        null, null, null, null);
  }

  private static final class EmptyQuery<T> extends AbstractQuery<T> {
    private EmptyQuery(final Builder<T> builder) {
      super(builder);
    }

    static <T> IQuery<T> of(final Class<T> entityClass) {
      return new Builder<T>(entityClass).build();
    }

    private static final class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
      Builder(final Class<T> entityClass) {
        super(entityClass);
      }

      @Override
      public IQuery<T> build() {
        return new EmptyQuery<>(this);
      }
    }
  }
}
