package de.yyuh.celery.api.messaging;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.Message;

public final class MessageBus {

  private final IMessagingProvider messagingProvider;
  private final String serviceId;

  private MessageBus(final Builder builder) {
    this.messagingProvider = builder.messagingProvider;
    this.serviceId = builder.serviceId;

    this.messagingProvider.subscribe(this.serviceId);
    this.messagingProvider.subscribe("*");
  }

  @NotNull
  public CompletableFuture<Void> broadcast(final @NotNull Message message) {
    return this.messagingProvider.publish("*", message);
  }

  @NotNull
  public CompletableFuture<Void> send(final @NotNull String channel, final @NotNull Message message) {
    return this.messagingProvider.publish(channel, message);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private IMessagingProvider messagingProvider;
    private String serviceId;

    private Builder() {
    }

    @NotNull
    public Builder messagingProvider(final @NotNull IMessagingProvider messagingProvider) {
      this.messagingProvider = messagingProvider;
      return this;
    }

    @NotNull
    public Builder serviceId(final @NotNull String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    @NotNull
    public MessageBus build() {
      return new MessageBus(this);
    }
  }

}
