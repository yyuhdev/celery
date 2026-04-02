package club.revived.celery.test.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.messaging.provider.StorageProvider;

/**
 * In-memory implementation of {@link StorageProvider} for testing purposes.
 * 
 * <p>This mock simulates Redis behavior without requiring an actual Redis instance:
 * <ul>
 *   <li>Key-value storage with optional TTL support</li>
 *   <li>Pattern-based key lookup</li>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * InMemoryStorageProvider storage = new InMemoryStorageProvider();
 * 
 * // Use in tests
 * Celery celery = Celery.builder()
 *     .storage(storage)
 *     .nodeId("test-node")
 *     .build();
 * 
 * // Verify stored data
 * assertThat(storage.getStoredKeys()).contains("my-key");
 * }</pre>
 */
public final class InMemoryStorageProvider implements StorageProvider {

  private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
  private final Map<String, Long> expirations = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private boolean connected = false;

  @Override
  public void connect(final @NotNull DatabaseCredentials creds) {
    this.connected = true;
  }

  @Override
  @NotNull
  public CompletableFuture<byte[]> get(final @NotNull String key) {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      checkExpiration(key);
      return storage.get(key);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> set(final @NotNull String key, final byte[] value) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      storage.put(key, value);
      expirations.remove(key);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> set(final @NotNull String key, final byte[] value, final long ttl) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      storage.put(key, value);
      
      final long expirationTime = System.currentTimeMillis() + (ttl * 1000);
      expirations.put(key, expirationTime);
      
      // Schedule cleanup
      scheduler.schedule(() -> {
        if (expirations.containsKey(key) && System.currentTimeMillis() >= expirations.get(key)) {
          storage.remove(key);
          expirations.remove(key);
        }
      }, ttl, TimeUnit.SECONDS);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(final @NotNull String key) {
    return CompletableFuture.runAsync(() -> {
      checkConnection();
      storage.remove(key);
      expirations.remove(key);
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<String>> keys(final @NotNull String pattern) {
    return CompletableFuture.supplyAsync(() -> {
      checkConnection();
      
      // Clean expired keys first
      cleanExpiredKeys();
      
      final List<String> matchingKeys = new ArrayList<>();
      final String regexPattern = globToRegex(pattern);
      
      for (final String key : storage.keySet()) {
        if (key.matches(regexPattern)) {
          matchingKeys.add(key);
        }
      }
      
      return matchingKeys;
    });
  }

  // ==================== Test Helper Methods ====================

  /**
   * Returns all stored keys (for test verification).
   * 
   * @return list of all keys
   */
  @NotNull
  public List<String> getStoredKeys() {
    cleanExpiredKeys();
    return new ArrayList<>(storage.keySet());
  }

  /**
   * Returns the raw byte value for a key (for test verification).
   * 
   * @param key the key
   * @return the value or null if not found
   */
  public byte[] getRaw(final @NotNull String key) {
    checkExpiration(key);
    return storage.get(key);
  }

  /**
   * Checks if a key exists (for test verification).
   * 
   * @param key the key
   * @return true if the key exists
   */
  public boolean containsKey(final @NotNull String key) {
    checkExpiration(key);
    return storage.containsKey(key);
  }

  /**
   * Returns the number of stored keys (for test verification).
   * 
   * @return the count
   */
  public int size() {
    cleanExpiredKeys();
    return storage.size();
  }

  /**
   * Clears all stored data (for test cleanup).
   */
  public void clear() {
    storage.clear();
    expirations.clear();
  }

  /**
   * Checks if the provider is connected.
   * 
   * @return true if connected
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Simulates disconnection (for testing error scenarios).
   */
  public void disconnect() {
    connected = false;
  }

  /**
   * Shuts down the scheduler. Call this in test cleanup.
   */
  public void shutdown() {
    scheduler.shutdownNow();
  }

  // ==================== Internal Methods ====================

  private void checkConnection() {
    if (!connected) {
      throw new IllegalStateException("InMemoryStorageProvider is not connected");
    }
  }

  private void checkExpiration(final String key) {
    final Long expiration = expirations.get(key);
    if (expiration != null && System.currentTimeMillis() >= expiration) {
      storage.remove(key);
      expirations.remove(key);
    }
  }

  private void cleanExpiredKeys() {
    final long now = System.currentTimeMillis();
    expirations.entrySet().removeIf(entry -> {
      if (now >= entry.getValue()) {
        storage.remove(entry.getKey());
        return true;
      }
      return false;
    });
  }

  /**
   * Converts a Redis glob pattern to a Java regex pattern.
   * Supports: * (any characters), ? (single character), [...] (character class)
   */
  private String globToRegex(final String glob) {
    final StringBuilder regex = new StringBuilder("^");
    
    for (int i = 0; i < glob.length(); i++) {
      final char c = glob.charAt(i);
      
      switch (c) {
        case '*' -> regex.append(".*");
        case '?' -> regex.append(".");
        case '.' -> regex.append("\\.");
        case '\\' -> regex.append("\\\\");
        case '[' -> regex.append("[");
        case ']' -> regex.append("]");
        case '^' -> regex.append("\\^");
        case '$' -> regex.append("\\$");
        case '(' -> regex.append("\\(");
        case ')' -> regex.append("\\)");
        case '{' -> regex.append("\\{");
        case '}' -> regex.append("\\}");
        case '|' -> regex.append("\\|");
        case '+' -> regex.append("\\+");
        default -> regex.append(c);
      }
    }
    
    regex.append("$");
    return regex.toString();
  }
}
