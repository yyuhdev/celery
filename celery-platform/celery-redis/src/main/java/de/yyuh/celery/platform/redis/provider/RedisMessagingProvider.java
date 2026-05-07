package de.yyuh.celery.platform.redis.provider;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

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
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Redis Pub/Sub messaging provider.
 *
 * <p>This provider connects to a single Redis instance via Lettuce and
 * uses Redis Pub/Sub for message distribution. Messages are encoded as
 * Protobuf byte arrays. Incoming messages are unpacked via the
 * {@link MessageRegistry} and dispatched through the {@link EventBus}.
 *
 * <p>Auto-reconnect is supported through {@link IReconnectable}.
 */
public final class RedisMessagingProvider implements IReconnectable, IMessagingProvider {

  private RedisPubSubAsyncCommands<byte[], byte[]> asyncCommands;
  private StatefulRedisPubSubConnection<byte[], byte[]> connection;

  @Inject
  private EventBus eventBus;

  @Inject
  private MessageRegistry messageRegistry;

  /** {@inheritDoc} */
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

      this.connection = client.connectPubSub(ByteArrayCodec.INSTANCE);
      this.asyncCommands = this.connection.async();

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
    this.connection.addListener(new RedisPubSubListener<>() {
      /** {@inheritDoc} */
      @Override
      public void message(final byte[] ch, final byte[] body) {
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
      public void message(byte[] pattern, byte[] ch, byte[] body) {
      }

      /** {@inheritDoc} */
      @Override
      public void subscribed(byte[] ch, long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void psubscribed(byte[] pattern, long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void unsubscribed(byte[] ch, long count) {
      }

      /** {@inheritDoc} */
      @Override
      public void punsubscribed(byte[] pattern, long count) {
      }
    });

    return this.asyncCommands.subscribe(channel.getBytes())
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> unsubscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> this.asyncCommands
        .unsubscribe(channel.getBytes())
        .toCompletableFuture()
        .join());
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    if (this.connection != null) {
      this.connection.close();
    }
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> this.connection != null && this.connection.isOpen());
  }
}
