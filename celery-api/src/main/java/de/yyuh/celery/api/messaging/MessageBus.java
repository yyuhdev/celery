package de.yyuh.celery.api.messaging;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.Message;

/**
 * Message bus for publishing and consuming messages.
 */
public final class MessageBus {

  private final IMessagingProvider messagingProvider;
  private final String serviceId;

  private MessageBus(final Builder builder) {
    this.messagingProvider = builder.messagingProvider;
    this.serviceId = builder.serviceId;

    this.messagingProvider.subscribe(this.serviceId);
    this.messagingProvider.subscribe("*");
  }

  /**
   * Broadcasts a message to all subscribers.
   *
   * @param message the message to broadcast
   * @return a CompletableFuture that completes when the message is published
   */
  @NotNull
  public CompletableFuture<Void> broadcast(final @NotNull Message message) {
    return this.messagingProvider.publish("*", message);
  }

  /**
   * Sends a message to a specific channel.
   *
   * @param channel the channel to send the message to
   * @param message the message to send
   * @return a CompletableFuture that completes when the message is published
   */
  @NotNull
  public CompletableFuture<Void> send(final @NotNull String channel, final @NotNull Message message) {
    return this.messagingProvider.publish(channel, message);
  }

  /**
   * Creates a new MessageBus builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating MessageBus instances.
   */
  public static final class Builder {
    private IMessagingProvider messagingProvider;
    private String serviceId;

    private Builder() {
    }

    /**
     * Sets the messaging provider.
     *
     * @param messagingProvider the messaging provider to use
     * @return this builder for chaining
     */
    @NotNull
    public Builder messagingProvider(final @NotNull IMessagingProvider messagingProvider) {
      this.messagingProvider = messagingProvider;
      return this;
    }

    /**
     * Sets the service ID.
     *
     * @param serviceId the service identifier
     * @return this builder for chaining
     */
    @NotNull
    public Builder serviceId(final @NotNull String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * Builds the MessageBus.
     *
     * @return the built MessageBus
     */
    @NotNull
    public MessageBus build() {
      return new MessageBus(this);
    }
  }

}
