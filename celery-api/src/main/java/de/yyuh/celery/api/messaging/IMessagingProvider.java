package de.yyuh.celery.api.messaging;

import com.google.protobuf.Message;
import de.yyuh.celery.api.provider.IProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Provider for messaging operations.
 */
public interface IMessagingProvider extends IProvider {

  /**
   * Publishes a message to the specified channel.
   *
   * @param channel the channel to publish to
   * @param message the message to publish
   * @return a CompletableFuture that completes when the message is published
   */
  @NotNull
  CompletableFuture<Void> publish(final @NotNull String channel, final @NotNull Message message);

  /**
   * Subscribes to the specified channel.
   *
   * @param channel the channel to subscribe to
   * @return a CompletableFuture that completes when the subscription is complete
   */
  @NotNull
  CompletableFuture<Void> subscribe(final @NotNull String channel);

  /**
   * Unsubscribes from the specified channel.
   *
   * @param channel the channel to unsubscribe from
   * @return a CompletableFuture that completes when the unsubscription is complete
   */
  @NotNull
  CompletableFuture<Void> unsubscribe(final @NotNull String channel);
}
