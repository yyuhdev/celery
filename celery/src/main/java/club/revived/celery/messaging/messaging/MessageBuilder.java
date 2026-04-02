package club.revived.celery.messaging.messaging;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.protobuf.Message;

import club.revived.celery.messaging.Concordia;
import club.revived.proto.v1.minigames.Envelope;
import club.revived.proto.v1.minigames.ExtensionsProto;

public final class MessageBuilder<T extends Message> {

  @NotNull
  private final T payload;

  @Nullable
  private String target;

  @Nullable
  private Duration timeout;

  @Nullable
  private Runnable onTimeout;

  @Nullable
  private Consumer<Envelope> onAck;

  private MessageBuilder(final @NotNull T payload) {
    this.payload = payload;
  }

  @NotNull
  public static <T extends Message> MessageBuilder<T> of(final @NotNull T payload) {
    return new MessageBuilder<>(payload);
  }

  @NotNull
  public MessageBuilder<T> target(final @NotNull String target) {
    this.target = target;
    return this;
  }

  @NotNull
  public MessageBuilder<T> timeout(final @NotNull Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  @NotNull
  public MessageBuilder<T> onTimeout(final @NotNull Runnable onTimeout) {
    this.onTimeout = onTimeout;
    return this;
  }

  @NotNull
  public MessageBuilder<T> onAck(final @NotNull Consumer<Envelope> onAck) {
    this.onAck = onAck;
    return this;
  }

  public void send() {
    final var correlationId = UUID.randomUUID().toString();
    final var envelope = Envelope.newBuilder()
        .setCorrelationId(correlationId)
        .setSender(Concordia.instance().nodeId())
        .setTarget(target != null ? target : "*")
        .setPayload(payload.toByteString())
        .setIsRequest(onAck != null)
        .build();

    if (onAck != null) {
      if (timeout == null) {
        throw new IllegalStateException("Timeout must be set for Acknowledgements");
      }

      if (onTimeout == null) {
        throw new IllegalStateException("onTimeout must be set for Acknowledgements");
      }

      Concordia.instance().messageManager().registerAck(
          correlationId,
          timeout,
          onTimeout,
          onAck);
    }

    Concordia.instance().pubSubProvider().publish(getTopicFromOptions(payload), envelope.toByteArray());
  }

  @NotNull
  private String getTopicFromOptions(final @NotNull T message) {
    final var descriptor = message.getDescriptorForType();
    final var options = descriptor.getOptions();

    return options.getExtension(ExtensionsProto.pubsubTopic);
  }
}
