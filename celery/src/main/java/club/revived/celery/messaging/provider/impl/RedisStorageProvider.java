package club.revived.celery.messaging.provider.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.messaging.provider.StorageProvider;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

public final class RedisStorageProvider implements StorageProvider {

  @Nullable
  private RedisAsyncCommands<byte[], byte[]> asyncCommands;

  @Override
  public void connect(final @NotNull DatabaseCredentials credentials) {
    final String url;
    if (credentials.password() != null && !credentials.password().isBlank()) {
      url = "redis://" + credentials.password() + "@" + credentials.host() + ":" + credentials.port();
    } else {
      url = "redis://" + credentials.host() + ":" + credentials.port();
    }

    final var client = RedisClient.create(url);
    final StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE);
    asyncCommands = connection.async();
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(final @NotNull String key) {
    requireConnection();
    return asyncCommands.del(key.getBytes())
        .toCompletableFuture()
        .thenApply(_ -> null);
  }

  @Override
  @NotNull
  public CompletableFuture<byte @Nullable []> get(final @NotNull String key) {
    requireConnection();
    return asyncCommands.get(key.getBytes())
        .toCompletableFuture();
  }

  @Override
  @NotNull
  public CompletableFuture<Void> set(
      final @NotNull String key,
      final byte @NotNull [] value) {
    requireConnection();
    return asyncCommands.set(key.getBytes(), value)
        .toCompletableFuture()
        .thenApply(_ -> null);
  }

  @Override
  @NotNull
  public CompletableFuture<Void> set(
      final @NotNull String key,
      final byte @NotNull [] value,
      final long ttl) {
    requireConnection();
    return asyncCommands.setex(key.getBytes(), ttl, value)
        .toCompletableFuture()
        .thenApply(_ -> null);
  }

  @Override
  @NotNull
  public CompletableFuture<List<String>> keys(final @NotNull String pattern) {
    requireConnection();
    return asyncCommands.keys(pattern)
        .toCompletableFuture()
        .thenApply(keys -> keys.stream()
            .map(String::new)
            .toList());
  }

  private void requireConnection() {
    if (asyncCommands == null) {
      throw new IllegalStateException("Redis is not connected");
    }
  }
}
