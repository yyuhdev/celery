package de.yyuh.celery.platform.redis.cluster.provider;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.event.EventBus;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.messaging.MessageRegistry;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.injection.Inject;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubListener;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;

/**
 * Redis Cluster messaging provider that leverages Redis Cluster Pub/Sub.
 *
 * <p>
 * This provider connects to a Redis Cluster and publishes messages across
 * all cluster nodes. Subscriptions are cluster-aware and messages published
 * to any master node are propagated to subscribers connected to any node
 * in the cluster.
 *
 * <p>
 * The node URIs are derived from the credentials: the {@code ip} field may
 * contain comma-separated host:port pairs, or a single host with the port
 * from {@code Credentials.port()}.
 */
public final class RedisClusterMessagingProvider implements IReconnectable, IMessagingProvider {

  private RedisClusterPubSubAsyncCommands<byte[], byte[]> asyncCommands;
  private StatefulRedisClusterPubSubConnection<byte[], byte[]> connection;
  private RedisClusterClient client;

  @Inject
  private EventBus eventBus;

  @Inject
  private MessageRegistry messageRegistry;

  private final Map<String, RedisClusterPubSubListener<byte[], byte[]>> listeners = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final RedisURI[] uris = parseNodeURIs(credentials);

      this.client = RedisClusterClient.create(Arrays.asList(uris));
      this.connection = this.client.connectPubSub(ByteArrayCodec.INSTANCE);
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

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> publish(
      final @NotNull String channel,
      final @NotNull Message message) {
    return this.asyncCommands.publish(channel.getBytes(), message.toByteArray())
        .toCompletableFuture()
        .thenApply(status -> null);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> subscribe(final @NotNull String channel) {
    final RedisClusterPubSubListener<byte[], byte[]> listener = new RedisClusterPubSubListener<byte[], byte[]>() {
      /** {@inheritDoc} */
      @Override
      public void message(final RedisClusterNode node, final byte[] ch, final byte[] body) {
        if (Arrays.equals(ch, channel.getBytes())) {
          try {
            final var message = messageRegistry.unpack(body);
            eventBus.publish(message);

          } catch (final InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }
      }

      /** {@inheritDoc} */
      @Override
      public void message(final RedisClusterNode node, final byte[] pattern, final byte[] ch, final byte[] body) {
      }

      /** {@inheritDoc} */
      @Override
      public void subscribed(final RedisClusterNode node, final byte[] ch, final long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void psubscribed(final RedisClusterNode node, final byte[] pattern, final long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void unsubscribed(final RedisClusterNode node, final byte[] ch, final long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void punsubscribed(final RedisClusterNode node, final byte[] pattern, final long count) {
      }
    };

    this.listeners.put(channel, listener);
    this.connection.addListener(listener);

    return this.asyncCommands.subscribe(channel.getBytes())
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> unsubscribe(final @NotNull String channel) {
    final var listener = this.listeners.remove(channel);

    if (listener != null) {
      this.connection.removeListener(listener);
    }

    return this.asyncCommands.unsubscribe(channel.getBytes())
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    if (this.connection != null) {
      this.connection.close();
    }
    if (this.client != null) {
      this.client.shutdown();
    }
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> this.connection != null && this.connection.isOpen());
  }

  /**
   * Parses RedisURI array from credentials. The IP field may contain a single
   * host or a comma-separated list of {@code host:port} pairs for cluster nodes.
   */
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
