package club.revived.celery.messaging.provider.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import club.revived.celery.database.model.DatabaseCredentials;
import club.revived.celery.messaging.provider.PubSubProvider;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

public final class NATSPubSubProvider implements PubSubProvider {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

  @Nullable
  private Connection connection;

  @Override
  public void connect(final @NotNull DatabaseCredentials credentials) {
    try {
      final var url = "nats://" + credentials.host() + ":" + credentials.port();

      final Options options;

      if (credentials.password() == null) {
        options = new Options.Builder()
            .server(url)
            .connectExecutor(executorService)
            .build();
      } else {
        options = new Options.Builder()
            .server(url)
            .token(credentials.password().toCharArray())
            .connectExecutor(executorService)
            .build();
      }

      connection = Nats.connect(options);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to connect to NATS server at " + credentials.host() + ":" + credentials.port(),
          e);
    }
  }

  @Override
  @NotNull
  public CompletableFuture<Void> publish(
      final @NotNull String channel,
      final byte @NotNull [] message) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();
      try {
        connection.publish(channel, message);
      } catch (final Exception e) {
        throw new RuntimeException("Failed to publish message to channel " + channel, e);
      }
    }, executorService);
  }

  @Override
  @NotNull
  public CompletableFuture<Void> subscribe(
      final @NotNull String channel,
      final @NotNull Consumer<byte[]> handler) {
    return CompletableFuture.runAsync(() -> {
      requireConnection();
      try {
        final var dispatcher = connection.createDispatcher();
        dispatcher.subscribe(channel, msg -> handler.accept(msg.getData()));
      } catch (final Exception e) {
        throw new RuntimeException("Failed to subscribe to channel " + channel, e);
      }
    }, executorService);
  }

  private void requireConnection() {
    if (connection == null) {
      throw new IllegalStateException("NATS is not connected");
    }
  }
}
