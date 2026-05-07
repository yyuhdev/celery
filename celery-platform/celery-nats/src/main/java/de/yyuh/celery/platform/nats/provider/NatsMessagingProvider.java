package de.yyuh.celery.platform.nats.provider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.Message;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.event.EventBus;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.celery.api.messaging.MessageRegistry;
import de.yyuh.celery.api.provider.IReconnectable;
import de.yyuh.libs.core.injection.Inject;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Connection.Status;

/**
 * NATS implementation of {@link IMessagingProvider} for Pub/Sub messaging.
 *
 * <p>This provider connects to a single NATS server and supports publishing
 * and subscribing to channels. Messages are encoded as Protobuf byte arrays.
 * Incoming messages are unpacked via the {@link MessageRegistry} and dispatched
 * through the {@link EventBus}.
 *
 * <p>Auto-reconnect is supported through {@link IReconnectable}.
 */
public final class NatsMessagingProvider implements IReconnectable, IMessagingProvider {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private Connection connection;

  private final Map<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();

  @Inject
  private EventBus eventBus;

  @Inject
  private MessageRegistry messageRegistry;

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> publish(
      final @NotNull String channel,
      final @NotNull Message message) {
    return CompletableFuture.runAsync(() -> this.connection.publish(channel, message.toByteArray()),
        this.executorService);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> subscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> {
      final var dispatcher = this.connection.createDispatcher();
      dispatcher.subscribe(channel, msg -> Result.of(() -> {
        final var message = this.messageRegistry.unpack(msg.getData());

        this.eventBus.publish(message);

        return null;
      }));

      this.dispatchers.put(channel, dispatcher);
    }, this.executorService);
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> unsubscribe(final @NotNull String channel) {
    return CompletableFuture.runAsync(() -> {
      final var dispatcher = this.dispatchers.get(channel);

      if (dispatcher == null) {
        return;
      }

      this.connection.closeDispatcher(dispatcher);
    }, this.executorService);
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    Result.of(() -> {
      this.connection.close();

      return null;
    });
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final @NotNull Credentials credentials) {
    final var timer = Timer.start();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final String url = "nats://" + credentials.ip() + ":" + credentials.port();

      final var options = new Options.Builder()
          .server(url)
          .token(credentials.password().toCharArray())
          .connectExecutor(this.executorService)
          .build();

      this.connection = Nats.connect(options);

      return timer.end();
    }).mapErr(Exception::getMessage));
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> {
      if (this.connection == null) {
        return false;
      }

      final var status = this.connection.getStatus();

      return status == Status.CONNECTED;
    }, this.executorService);
  }
}
