package de.yyuh.celery.api.messaging;

import com.google.protobuf.Message;
import de.yyuh.celery.api.provider.IProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface IMessagingProvider extends IProvider {

  @NotNull
  CompletableFuture<Void> publish(final @NotNull String channel, final @NotNull Message message);

  @NotNull
  CompletableFuture<Void> subscribe(final @NotNull String channel);

  @NotNull
  CompletableFuture<Void> unsubscribe(final @NotNull String channel);
}
