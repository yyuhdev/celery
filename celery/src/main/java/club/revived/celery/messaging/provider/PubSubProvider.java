package club.revived.celery.messaging.provider;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import club.revived.celery.database.model.DatabaseCredentials;

public interface PubSubProvider {

  void connect(final @NotNull DatabaseCredentials credentials);

  @NotNull
  CompletableFuture<Void> publish(final @NotNull String channel, final byte @NotNull [] message);

  @NotNull
  CompletableFuture<Void> subscribe(final @NotNull String channel, final @NotNull Consumer<byte[]> handler);
}
