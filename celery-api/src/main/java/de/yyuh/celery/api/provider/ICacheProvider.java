package de.yyuh.celery.api.provider;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provider for cache operations (get, set, delete, exists).
 *
 * <p>Implementations provide key-value cache semantics backed by
 * systems like Redis or DragonflyDB. All operations are asynchronous
 * and return {@link CompletableFuture}.
 *
 * @see IProvider
 */
public interface ICacheProvider extends IProvider {

  /**
   * Stores a value with a time-to-live.
   *
   * @param key   the cache key
   * @param value the byte array value to store
   * @param ttl   the time-to-live duration before the entry expires
   * @return a future that completes when the value is stored
   */
  @NotNull
  CompletableFuture<Void> set(final @NotNull String key, final byte @NotNull [] value, final @NotNull Duration ttl);

  /**
   * Retrieves a value by key.
   *
   * @param key the cache key
   * @return a future containing the value if present, or empty if absent
   */
  @NotNull
  CompletableFuture<Optional<byte[]>> get(final @NotNull String key);

  /**
   * Deletes a value by key.
   *
   * @param key the cache key
   * @return a future that completes when the entry is deleted
   */
  @NotNull
  CompletableFuture<Void> delete(final @NotNull String key);

  /**
   * Checks whether a key exists in the cache.
   *
   * @param key the cache key
   * @return a future containing {@code true} if the key exists
   */
  @NotNull
  CompletableFuture<Boolean> exists(final @NotNull String key);
}