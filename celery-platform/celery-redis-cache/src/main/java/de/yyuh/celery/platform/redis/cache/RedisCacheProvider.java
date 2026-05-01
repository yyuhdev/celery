package de.yyuh.celery.platform.redis.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public class RedisCacheProvider implements IReconnectable, ICacheProvider {

  private RedisAsyncCommands<byte[], byte[]> asyncCommands;
  private StatefulRedisConnection<byte[], byte[]> connection;

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final var uriBuilder = RedisURI.builder()
          .withHost(credentials.ip())
          .withPort(credentials.port());

      if (credentials.password() != null && !credentials.password().isBlank()) {
        uriBuilder.withPassword(credentials.password().toCharArray());
      }

      final RedisClient client = RedisClient.create(uriBuilder.build());

      this.connection = client.connect(ByteArrayCodec.INSTANCE);
      this.asyncCommands = this.connection.async();

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  @Override
  public @NotNull CompletableFuture<Void> set(
      final @NotNull String key,
      final byte @NotNull [] value,
      final @NotNull Duration ttl) {
    return this.asyncCommands.setex(key.getBytes(), ttl.getSeconds(), value)
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  @Override
  public @NotNull CompletableFuture<Optional<byte[]>> get(final @NotNull String key) {
    return this.asyncCommands.get(key.getBytes())
        .toCompletableFuture()
        .thenApply(o -> Optional.ofNullable(o));
  }

  @Override
  public @NotNull CompletableFuture<Void> delete(final @NotNull String key) {
    return this.asyncCommands.del(key.getBytes())
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  @Override
  public @NotNull CompletableFuture<Boolean> exists(final @NotNull String key) {
    return this.asyncCommands.exists(key.getBytes())
        .toCompletableFuture()
        .thenApply(count -> count > 0);
  }

  @Override
  public void close() {
    if (this.connection != null) {
      this.connection.close();
    }
  }

  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> this.connection != null && this.connection.isOpen());
  }
}
