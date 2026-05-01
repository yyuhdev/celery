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
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class RedisMessagingProvider implements IReconnectable, IMessagingProvider {

  private RedisPubSubAsyncCommands<byte[], byte[]> asyncCommands;
  private StatefulRedisPubSubConnection<byte[], byte[]> connection;

  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final String url;

      if (credentials.password() != null && !credentials.password().isBlank()) {
        url = "redis://'" + credentials.password() + "'@" + credentials.ip() + ":" + credentials.port();
      } else {
        url = "redis://" + credentials.ip() + ":" + credentials.port();
      }

      final RedisClient client = RedisClient.create(url);

      this.connection = client.connectPubSub(ByteArrayCodec.INSTANCE);
      this.asyncCommands = this.connection.async();

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  @Override
  public @NotNull CompletableFuture<Void> publish(
      final @NotNull String channel,
      final @NotNull Message message) {
    return this.asyncCommands.publish(channel.getBytes(), message.toByteArray())
        .toCompletableFuture()
        .thenApply(status -> null);
  }

  @Override
  public @NotNull CompletableFuture<Void> subscribe(final @NotNull String channel) {
    this.connection.addListener(new RedisPubSubListener<>() {
      @Override
      public void message(final byte[] ch, final byte[] body) {
        if (Arrays.equals(ch, channel.getBytes())) {
          try {
            final var message = MessageRegistry.getInstance().unpack(body);

            EventBus.instance().publish(message);
          } catch (final InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }
      }

      @Override
      public void message(byte[] pattern, byte[] ch, byte[] body) {
      }

      @Override
      public void subscribed(byte[] ch, long count) {
      }

      @Override
      public void psubscribed(byte[] pattern, long count) {
      }

      @Override
      public void unsubscribed(byte[] ch, long count) {
      }

      @Override
      public void punsubscribed(byte[] pattern, long count) {
      }
    });

    return this.asyncCommands.subscribe(channel.getBytes())
        .toCompletableFuture()
        .thenApply(v -> null);
  }

  @Override
  public @NotNull CompletableFuture<Void> unsubscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> this.asyncCommands
        .unsubscribe(channel.getBytes())
        .toCompletableFuture()
        .join());
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
