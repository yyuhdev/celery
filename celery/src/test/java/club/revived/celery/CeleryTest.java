package club.revived.celery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.test.mock.InMemoryDatabaseProvider;
import club.revived.celery.test.mock.InMemoryPubSubProvider;
import club.revived.celery.test.mock.InMemoryStorageProvider;
import club.revived.celery.test.model.TestUser;

/**
 * Comprehensive test suite for the unified Celery library.
 */
@DisplayName("Celery")
class CeleryTest {

  private InMemoryStorageProvider storageProvider;
  private InMemoryPubSubProvider pubSubProvider;
  private InMemoryDatabaseProvider<TestUser> databaseProvider;

  @BeforeEach
  void setUp() {
    Celery.reset();
    storageProvider = new InMemoryStorageProvider();
    pubSubProvider = new InMemoryPubSubProvider();
    databaseProvider = new InMemoryDatabaseProvider<>();
  }

  @AfterEach
  void tearDown() {
    Celery.reset();
    if (storageProvider != null) {
      storageProvider.shutdown();
    }
  }

  @Nested
  @DisplayName("Builder")
  class BuilderTests {

    @Test
    @DisplayName("should build with minimal configuration")
    void shouldBuildWithMinimalConfig() {
      final Celery celery = Celery.builder()
          .nodeId("test-node")
          .build();

      assertThat(celery).isNotNull();
      assertThat(celery.nodeId()).isEqualTo("test-node");
    }

    @Test
    @DisplayName("should build with storage provider")
    void shouldBuildWithStorageProvider() {
      storageProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 6379, null));

      final Celery celery = Celery.builder()
          .storage(storageProvider)
          .nodeId("test-node")
          .build();

      assertThat(celery).isNotNull();
      assertThat(celery.storage()).isEqualTo(storageProvider);
    }

    @Test
    @DisplayName("should build with pub/sub provider")
    void shouldBuildWithPubSubProvider() {
      pubSubProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 4222, null));

      final Celery celery = Celery.builder()
          .pubSub(pubSubProvider)
          .nodeId("test-node")
          .build();

      assertThat(celery).isNotNull();
      assertThat(celery.pubSub()).isEqualTo(pubSubProvider);
    }

    @Test
    @DisplayName("should build with all providers")
    void shouldBuildWithAllProviders() {
      storageProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 6379, null));
      pubSubProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 4222, null));
      databaseProvider.connect(new club.revived.celery.database.model.DatabaseCredentials("user", "localhost", "pass", 27017, "test"));

      final Celery celery = Celery.builder()
          .storage(storageProvider)
          .pubSub(pubSubProvider)
          .database(TestUser.class, databaseProvider, Credentials.forMongo("user", "localhost", "pass", 27017, "test"))
          .nodeId("test-node")
          .build();

      assertThat(celery).isNotNull();
      assertThat(celery.storage()).isEqualTo(storageProvider);
      assertThat(celery.pubSub()).isEqualTo(pubSubProvider);
      assertThat(celery.database()).isNotNull();
    }

    @Test
    @DisplayName("should throw when building twice")
    void shouldThrowWhenBuildingTwice() {
      Celery.builder()
          .nodeId("test-node")
          .build();

      assertThatThrownBy(() -> Celery.builder())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("should use default node ID from environment")
    void shouldUseDefaultNodeId() {
      final Celery celery = Celery.builder().build();

      assertThat(celery.nodeId()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Instance")
  class InstanceTests {

    @Test
    @DisplayName("should throw when accessing instance before initialization")
    void shouldThrowWhenNotInitialized() {
      assertThatThrownBy(Celery::instance)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not been initialized");
    }

    @Test
    @DisplayName("should return instance after initialization")
    void shouldReturnInstanceAfterInit() {
      Celery.builder().nodeId("test").build();

      final Celery celery = Celery.instance();

      assertThat(celery).isNotNull();
      assertThat(celery.nodeId()).isEqualTo("test");
    }

    @Test
    @DisplayName("should reset instance")
    void shouldResetInstance() {
      Celery.builder().nodeId("test").build();
      
      Celery.reset();

      assertThatThrownBy(Celery::instance)
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Database Operations")
  class DatabaseOperationTests {

    @BeforeEach
    void setUpDatabase() {
      databaseProvider.connect(new club.revived.celery.database.model.DatabaseCredentials("user", "localhost", "pass", 27017, "test"));
      
      Celery.builder()
          .database(TestUser.class, databaseProvider, Credentials.forMongo("user", "localhost", "pass", 27017, "test"))
          .nodeId("test-node")
          .build();
    }

    @Test
    @DisplayName("should throw when database not configured")
    void shouldThrowWhenDatabaseNotConfigured() {
      Celery.reset();
      Celery.builder().nodeId("test").build();

      assertThatThrownBy(() -> Celery.instance().findAll(TestUser.class))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Database not configured");
    }
  }

  @Nested
  @DisplayName("Messaging Operations")
  class MessagingOperationTests {

    @Test
    @DisplayName("should throw when pubsub not configured")
    void shouldThrowWhenPubSubNotConfigured() {
      Celery.builder().nodeId("test").build();

      assertThatThrownBy(() -> Celery.instance().pubSub())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("PubSub not configured");
    }

    @Test
    @DisplayName("should throw when storage not configured")
    void shouldThrowWhenStorageNotConfigured() {
      Celery.builder().nodeId("test").build();

      assertThatThrownBy(() -> Celery.instance().storage())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Storage not configured");
    }
  }

  @Nested
  @DisplayName("Accessors")
  class AccessorTests {

    @BeforeEach
    void setUpCelery() {
      storageProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 6379, null));
      pubSubProvider.connect(new club.revived.celery.database.model.DatabaseCredentials(null, "localhost", null, 4222, null));

      Celery.builder()
          .storage(storageProvider)
          .pubSub(pubSubProvider)
          .nodeId("test-node")
          .build();
    }

    @Test
    @DisplayName("should return scheduler")
    void shouldReturnScheduler() {
      assertThat(Celery.instance().scheduler()).isNotNull();
    }

    @Test
    @DisplayName("should return message manager")
    void shouldReturnMessageManager() {
      assertThat(Celery.instance().messages()).isNotNull();
    }

    @Test
    @DisplayName("should return cache manager")
    void shouldReturnCacheManager() {
      assertThat(Celery.instance().cache()).isNotNull();
    }

    @Test
    @DisplayName("should return watcher")
    void shouldReturnWatcher() {
      assertThat(Celery.instance().watcher()).isNotNull();
    }

    @Test
    @DisplayName("should return gson")
    void shouldReturnGson() {
      assertThat(Celery.instance().gson()).isNotNull();
    }

    @Test
    @DisplayName("should return deprecated accessors")
    @SuppressWarnings("deprecation")
    void shouldReturnDeprecatedAccessors() {
      assertThat(Celery.instance().messageManager()).isNotNull();
      assertThat(Celery.instance().cacheManager()).isNotNull();
      assertThat(Celery.instance().pubSubProvider()).isNotNull();
      assertThat(Celery.instance().storageProvider()).isNotNull();
    }
  }
}
