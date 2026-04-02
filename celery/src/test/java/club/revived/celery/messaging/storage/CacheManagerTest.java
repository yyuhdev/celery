package club.revived.celery.messaging.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.Celery;
import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.test.mock.InMemoryPubSubProvider;
import club.revived.celery.test.mock.InMemoryStorageProvider;

/**
 * Test suite for CacheManager with mocked Redis storage.
 */
@DisplayName("CacheManager")
class CacheManagerTest {

  private InMemoryStorageProvider storageProvider;
  private InMemoryPubSubProvider pubSubProvider;
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    Celery.reset();
    
    storageProvider = new InMemoryStorageProvider();
    storageProvider.connect(new DatabaseCredentials(null, "localhost", null, 6379, null));
    
    pubSubProvider = new InMemoryPubSubProvider();
    pubSubProvider.connect(new DatabaseCredentials(null, "localhost", null, 4222, null));
    
    Celery.builder()
        .storage(storageProvider)
        .pubSub(pubSubProvider)
        .nodeId("test-node")
        .build();
    
    cacheManager = Celery.instance().cache();
  }

  @AfterEach
  void tearDown() {
    Celery.reset();
    storageProvider.shutdown();
  }

  // Test data record
  record TestData(String name, int value, boolean active) {}

  @Nested
  @DisplayName("Write Operations")
  class WriteOperationTests {

    @Test
    @DisplayName("should write JSON value to cache")
    void shouldWriteJsonValueToCache() {
      final TestData data = new TestData("test", 42, true);

      cacheManager.write("key", data).join();

      assertThat(storageProvider.containsKey("key")).isTrue();
    }

    @Test
    @DisplayName("should publish update notification on write")
    void shouldPublishUpdateNotificationOnWrite() {
      final TestData data = new TestData("test", 42, true);

      cacheManager.write("my-key", data).join();

      // Wait for async publish
      try { TimeUnit.MILLISECONDS.sleep(100); } catch (InterruptedException ignored) {}

      assertThat(pubSubProvider.getPublishedMessages("my-key:updates")).hasSize(1);
    }

    @Test
    @DisplayName("should write value with TTL")
    void shouldWriteValueWithTtl() {
      final TestData data = new TestData("test", 42, true);

      cacheManager.write("ttl-key", data, 60).join();

      assertThat(storageProvider.containsKey("ttl-key")).isTrue();
    }
  }

  @Nested
  @DisplayName("Read Operations")
  class ReadOperationTests {

    @Test
    @DisplayName("should read JSON value from cache")
    void shouldReadJsonValueFromCache() {
      final TestData original = new TestData("test", 42, true);
      cacheManager.write("key", original).join();

      final Optional<TestData> result = cacheManager.read("key", TestData.class).join();

      assertThat(result).isPresent();
      assertThat(result.get().name()).isEqualTo("test");
      assertThat(result.get().value()).isEqualTo(42);
      assertThat(result.get().active()).isTrue();
    }

    @Test
    @DisplayName("should return empty for non-existent key")
    void shouldReturnEmptyForNonExistentKey() {
      final Optional<TestData> result = cacheManager.read("non-existent", TestData.class).join();

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Batch Operations")
  class BatchOperationTests {

    @Test
    @DisplayName("should batch read multiple values")
    void shouldBatchReadMultipleValues() {
      cacheManager.write("key1", new TestData("data1", 1, true)).join();
      cacheManager.write("key2", new TestData("data2", 2, true)).join();
      cacheManager.write("key3", new TestData("data3", 3, false)).join();

      final List<TestData> results = cacheManager.batchRead(
          List.of("key1", "key2", "key3"), TestData.class).join();

      assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("should batch write multiple values")
    void shouldBatchWriteMultipleValues() {
      final List<CacheManager.Entry<?>> entries = List.of(
          new CacheManager.Entry<>("batch1", new TestData("data1", 1, true)),
          new CacheManager.Entry<>("batch2", new TestData("data2", 2, true)),
          new CacheManager.Entry<>("batch3", new TestData("data3", 3, false)));

      cacheManager.batchWrite(entries).join();

      assertThat(storageProvider.containsKey("batch1")).isTrue();
      assertThat(storageProvider.containsKey("batch2")).isTrue();
      assertThat(storageProvider.containsKey("batch3")).isTrue();
    }

    @Test
    @DisplayName("should skip non-existent keys in batch read")
    void shouldSkipNonExistentKeysInBatchRead() {
      cacheManager.write("exists1", new TestData("data1", 1, true)).join();
      cacheManager.write("exists2", new TestData("data2", 2, true)).join();

      final List<TestData> results = cacheManager.batchRead(
          List.of("exists1", "non-existent", "exists2"), TestData.class).join();

      assertThat(results).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperationTests {

    @Test
    @DisplayName("should delete value from cache")
    void shouldDeleteValueFromCache() {
      cacheManager.write("key", new TestData("test", 42, true)).join();

      cacheManager.delete("key").join();

      assertThat(storageProvider.containsKey("key")).isFalse();
    }

    @Test
    @DisplayName("should publish empty update notification on delete")
    void shouldPublishEmptyUpdateNotificationOnDelete() {
      cacheManager.write("del-key", new TestData("test", 42, true)).join();
      // Wait for async publish from write to complete
      try { TimeUnit.MILLISECONDS.sleep(100); } catch (InterruptedException ignored) {}
      pubSubProvider.clearHistory();

      cacheManager.delete("del-key").join();

      // Wait for async publish
      try { TimeUnit.MILLISECONDS.sleep(100); } catch (InterruptedException ignored) {}

      assertThat(pubSubProvider.getPublishedMessages("del-key:updates")).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Entry Record")
  class EntryRecordTests {

    @Test
    @DisplayName("should create entry with key and value")
    void shouldCreateEntryWithKeyAndValue() {
      final TestData data = new TestData("test", 42, true);
      final CacheManager.Entry<TestData> entry = new CacheManager.Entry<>("my-key", data);

      assertThat(entry.key()).isEqualTo("my-key");
      assertThat(entry.value()).isEqualTo(data);
    }
  }
}
