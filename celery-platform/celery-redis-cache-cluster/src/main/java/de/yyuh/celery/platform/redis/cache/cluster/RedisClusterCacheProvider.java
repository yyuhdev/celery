package de.yyuh.celery.platform.redis.cache.cluster;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.provider.ICacheProvider;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * Redis Cluster cache provider with cluster-aware operations.
 *
 * <p>
 * This provider connects to a Redis Cluster and performs cache operations
 * (get, set, delete, exists) that are automatically routed to the correct
 * shard based on the hash slot of each key. The Redis Cluster handles
 * sharding, replication, and automatic failover.
 *
 * <p>
 * Node URIs are derived from credentials: the {@code ip} field may contain
 * a comma-separated list of {@code host:port} pairs for all cluster nodes,
 * or a single host (any cluster node is sufficient for discovery).
 */
public final class RedisClusterCacheProvider implements IReconnectable, ICacheProvider {

  private RedisAdvancedClusterAsyncCommands<byte[], byte[]> asyncCommands;
  private StatefulRedisClusterConnection<byte[], byte[]> connection;
  private RedisClusterClient client;

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final var uris = parseNodeURIs(credentials);

      this.client = RedisClusterClient.create(Arrays.asList(uris));
      this.connection = this.client.connect(ByteArrayCodec.INSTANCE);
      this.asyncCommands = this.connection.async();

      boolean clusterReady = false;
      for (int i = 0; i < 60; i++) {
        try {
          final String info = this.connection.sync().clusterInfo();
          if (info.contains("cluster_state:ok")) {
            clusterReady = true;
            break;
          }
        } catch (Exception ignored) {
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while waiting for cluster", e);
        }
      }
      if (!clusterReady) {
        throw new RuntimeException("Redis cluster not ready after 60s");
      }

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
        .thenApply(Optional::ofNullable);
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
    if (this.client != null) {
      this.client.shutdown();
    }
  }

  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> this.connection != null && this.connection.isOpen());
  }

  private static RedisURI[] parseNodeURIs(final @NotNull Credentials credentials) {
    final String ipField = credentials.ip();

    if (ipField.contains(",")) {
      return Arrays.stream(ipField.split(","))
          .map(String::strip)
          .map(entry -> {
            final String[] parts = entry.split(":");
            final String host = parts[0];
            final int port = parts.length > 1 ? Integer.parseInt(parts[1]) : credentials.port();

            return buildUri(host, port, credentials.password());
          })
          .toArray(RedisURI[]::new);
    }

    return new RedisURI[] { buildUri(credentials.ip(), credentials.port(), credentials.password()) };
  }

  private static RedisURI buildUri(final String host, final int port, final String password) {
    final var builder = RedisURI.builder()
        .withHost(host)
        .withPort(port);

    if (password != null && !password.isBlank()) {
      builder.withPassword(password.toCharArray());
    }

    return builder.build();
  }
}
