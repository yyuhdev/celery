package club.revived.celery.messaging.messaging;

import club.revived.celery.messaging.Concordia;
import club.revived.proto.v1.minigames.Envelope;
import club.revived.proto.v1.minigames.ExtensionsProto;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class MessageManager {

  private final Map<String, AckHandler> pendingAcks = new ConcurrentHashMap<>();
  private final Map<String, Map<Class<? extends Message>, List<Handler<?>>>> handlers = new ConcurrentHashMap<>();

  public <T extends Message> void registerHandler(
      final @NotNull Class<T> type,
      final @NotNull Parser<T> parser,
      final @NotNull Consumer<T> consumer) {
    final var channel = resolveChannel(parser);

    handlers
        .computeIfAbsent(channel, _ -> new ConcurrentHashMap<>())
        .computeIfAbsent(type, _ -> new CopyOnWriteArrayList<>())
        .add(new Handler<>(parser, consumer));
  }

  public <T extends Message> void registerHandler(
      final @NotNull String channel,
      final @NotNull Class<T> type,
      final @NotNull Parser<T> parser,
      final @NotNull Consumer<T> consumer) {
    handlers
        .computeIfAbsent(channel, _ -> new ConcurrentHashMap<>())
        .computeIfAbsent(type, _ -> new CopyOnWriteArrayList<>())
        .add(new Handler<>(parser, consumer));
  }

  public void registerAck(
      final @NotNull String correlationId,
      final @NotNull Duration timeout,
      final @NotNull Runnable onTimeout,
      final @NotNull Consumer<Envelope> onAck) {
    final ScheduledFuture<?> timeoutTask = Concordia.instance().scheduler().schedule(() -> {
      final var handler = pendingAcks.remove(correlationId);

      if (handler != null) {
        onTimeout.run();
      }
    }, timeout.toMillis(), TimeUnit.MILLISECONDS);

    pendingAcks.put(correlationId, new AckHandler(onAck, timeoutTask));
  }

  @SuppressWarnings("unchecked")
  public void handleIncoming(
      final @NotNull String channel,
      final byte @NotNull [] data) {
    try {
      final var envelope = Envelope.parseFrom(data);

      if (!envelope.getTarget().equals("*")
          && !envelope.getTarget().equals(Concordia.instance().nodeId())) {
        return;
      }

      if (envelope.getIsAck()) {
        final var correlationId = envelope.getCorrelationId();
        final var handler = pendingAcks.remove(correlationId);

        if (handler != null) {
          handler.timeoutTask().cancel(false);
          handler.consumer().accept(envelope);
        }
        return;
      }

      final var channelHandlers = handlers.get(channel);

      if (channelHandlers != null) {
        for (final var entry : channelHandlers.entrySet()) {
          for (final var handler : entry.getValue()) {
            try {
              final var message = handler.parser().parseFrom(envelope.getPayload());

              if (entry.getKey().equals(message.getClass())) {
                ((Consumer<Message>) handler.consumer()).accept(message);
              }
            } catch (final Exception e) {
              throw new RuntimeException("Failed to handle message", e);
            }
          }
        }
      }

      if (envelope.getIsRequest()) {
        final var ack = Envelope.newBuilder()
            .setCorrelationId(envelope.getCorrelationId())
            .setSender(Concordia.instance().nodeId())
            .setTarget(envelope.getSender())
            .setIsAck(true)
            .build();

        Concordia.instance().pubSubProvider().publish(channel, ack.toByteArray());
      }
    } catch (final Exception ignored) {
    }
  }

  @NotNull
  private static <T extends Message> String resolveChannel(final @NotNull Parser<T> parser) {
    try {
      final T defaultInstance = parser.parseFrom(ByteString.EMPTY);
      final var descriptor = defaultInstance.getDescriptorForType();
      final var options = descriptor.getOptions();

      if (!options.hasExtension(ExtensionsProto.pubsubTopic)) {
        throw new IllegalStateException(
            "Message " + descriptor.getFullName() + " does not define (pubsub_topic)");
      }

      return options.getExtension(ExtensionsProto.pubsubTopic);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to resolve channel from parser", e);
    }
  }

  private record AckHandler(
      @NotNull Consumer<Envelope> consumer,
      @NotNull ScheduledFuture<?> timeoutTask) {
  }

  private record Handler<T extends Message>(
      @NotNull Parser<T> parser,
      @NotNull Consumer<T> consumer) {
  }
}
