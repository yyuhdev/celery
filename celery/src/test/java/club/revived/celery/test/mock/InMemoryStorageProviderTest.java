package club.revived.celery.test.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import club.revived.celery.database.model.DatabaseCredentials;

/**
 * Test suite for InMemoryStorageProvider.
 */
@DisplayName("InMemoryStorageProvider")
class InMemoryStorageProviderTest {

  private InMemoryStorageProvider provider;

  @BeforeEach
  void setUp() {
    provider = new InMemoryStorageProvider();
    provider.connect(new DatabaseCredentials(null, "localhost", null, 6379, null));
  }

  @AfterEach
  void tearDown() {
    provider.shutdown();
  }

  @Nested
  @DisplayName("Connection")
  class ConnectionTests {

    @Test
    @DisplayName("should connect successfully")
    void shouldConnectSuccessfully() {
      assertThat(provider.isConnected()).isTrue();
    }

    @Test
    @DisplayName("should disconnect")
    void shouldDisconnect() {
      provider.disconnect();

      assertThat(provider.isConnected()).isFalse();
    }

    @Test
    @DisplayName("should throw when not connected")
    void shouldThrowWhenNotConnected() {
      provider.disconnect();

      assertThatThrownBy(() -> provider.get("key").join())
          .hasCauseInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Basic Operations")
  class BasicOperationTests {

    @Test
    @DisplayName("should set and get value")
    void shouldSetAndGetValue() {
      final byte[] value = "test-value".getBytes();

      provider.set("key", value).join();
      final byte[] result = provider.get("key").join();

      assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("should return null for non-existent key")
    void shouldReturnNullForNonExistentKey() {
      final byte[] result = provider.get("non-existent").join();

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should delete value")
    void shouldDeleteValue() {
      provider.set("key", "value".getBytes()).join();

      provider.delete("key").join();

      assertThat(provider.get("key").join()).isNull();
    }

    @Test
    @DisplayName("should overwrite existing value")
    void shouldOverwriteExistingValue() {
      provider.set("key", "value1".getBytes()).join();
      provider.set("key", "value2".getBytes()).join();

      final byte[] result = provider.get("key").join();

      assertThat(new String(result)).isEqualTo("value2");
    }
  }

  @Nested
  @DisplayName("TTL Operations")
  class TtlOperationTests {

    @Test
    @DisplayName("should set value with TTL")
    void shouldSetValueWithTtl() {
      provider.set("key", "value".getBytes(), 60).join();

      assertThat(provider.get("key").join()).isNotNull();
    }

    @Test
    @DisplayName("should expire value after TTL")
    void shouldExpireValueAfterTtl() throws InterruptedException {
      provider.set("key", "value".getBytes(), 1).join();

      // Wait for expiration
      TimeUnit.SECONDS.sleep(2);

      assertThat(provider.get("key").join()).isNull();
    }
  }

  @Nested
  @DisplayName("Key Pattern Matching")
  class KeyPatternTests {

    @BeforeEach
    void setUpKeys() {
      provider.set("user:1", "data1".getBytes()).join();
      provider.set("user:2", "data2".getBytes()).join();
      provider.set("user:10", "data10".getBytes()).join();
      provider.set("product:1", "prod1".getBytes()).join();
      provider.set("cache:user:1", "cache1".getBytes()).join();
    }

    @Test
    @DisplayName("should find keys with wildcard prefix")
    void shouldFindKeysWithWildcardPrefix() {
      final List<String> keys = provider.keys("user:*").join();

      assertThat(keys).containsExactlyInAnyOrder("user:1", "user:2", "user:10");
    }

    @Test
    @DisplayName("should find all keys with double wildcard")
    void shouldFindAllKeysWithDoubleWildcard() {
      final List<String> keys = provider.keys("*").join();

      assertThat(keys).hasSize(5);
    }

    @Test
    @DisplayName("should find keys with single character wildcard")
    void shouldFindKeysWithSingleCharWildcard() {
      final List<String> keys = provider.keys("user:?").join();

      assertThat(keys).containsExactlyInAnyOrder("user:1", "user:2");
    }

    @Test
    @DisplayName("should find keys with middle wildcard")
    void shouldFindKeysWithMiddleWildcard() {
      final List<String> keys = provider.keys("*:user:*").join();

      assertThat(keys).containsExactlyInAnyOrder("cache:user:1");
    }

    @Test
    @DisplayName("should return empty list for no matches")
    void shouldReturnEmptyListForNoMatches() {
      final List<String> keys = provider.keys("nonexistent:*").join();

      assertThat(keys).isEmpty();
    }
  }

  @Nested
  @DisplayName("Test Helpers")
  class TestHelperTests {

    @Test
    @DisplayName("should return stored keys")
    void shouldReturnStoredKeys() {
      provider.set("key1", "value1".getBytes()).join();
      provider.set("key2", "value2".getBytes()).join();

      final List<String> keys = provider.getStoredKeys();

      assertThat(keys).containsExactlyInAnyOrder("key1", "key2");
    }

    @Test
    @DisplayName("should get raw value")
    void shouldGetRawValue() {
      final byte[] value = "test".getBytes();
      provider.set("key", value).join();

      final byte[] raw = provider.getRaw("key");

      assertThat(raw).isEqualTo(value);
    }

    @Test
    @DisplayName("should check if key exists")
    void shouldCheckIfKeyExists() {
      provider.set("exists", "value".getBytes()).join();

      assertThat(provider.containsKey("exists")).isTrue();
      assertThat(provider.containsKey("not-exists")).isFalse();
    }

    @Test
    @DisplayName("should return size")
    void shouldReturnSize() {
      provider.set("key1", "value1".getBytes()).join();
      provider.set("key2", "value2".getBytes()).join();

      assertThat(provider.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("should clear all data")
    void shouldClearAllData() {
      provider.set("key1", "value1".getBytes()).join();
      provider.set("key2", "value2".getBytes()).join();

      provider.clear();

      assertThat(provider.size()).isZero();
    }
  }
}
