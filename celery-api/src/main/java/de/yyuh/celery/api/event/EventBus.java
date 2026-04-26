package de.yyuh.celery.api.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;

import com.google.protobuf.Message;

/**
 * A high-performance, reflection-free event bus for Protobuf {@link Message}
 * types.
 *
 * <p>
 * Handlers are registered as {@link IEventHandler} lambdas and dispatched
 * synchronously
 * in the order of registration. Lookup is O(1) via a {@link ConcurrentHashMap}
 * keyed
 * on the concrete message class.
 *
 * <p>
 * Thread-safety: subscribing, unsubscribing and publishing are all safe to call
 * from multiple threads concurrently. Iteration during
 * {@link #publish(Message)} never
 * blocks writers because the underlying list is a {@link CopyOnWriteArrayList}.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * EventBus bus = new EventBus();
 * bus.subscribe(PlayerJoinProto.class, e -> System.out.println("joined: " + e.getName()));
 * bus.publish(PlayerJoinProto.newBuilder().setName("Alice").build());
 * }</pre>
 */
public final class EventBus {

  private final Map<Class<?>, List<IEventHandler<? super Message>>> handlers = new ConcurrentHashMap<>();
  private final ExecutorService srv = Executors.newVirtualThreadPerTaskExecutor();

  private static EventBus instance;

  private EventBus() {
  }

  /**
   * Returns the singleton EventBus instance.
   *
   * @return the EventBus instance
   */
  public static EventBus instance() {
    if (instance == null) {
      instance = new EventBus();
    }

    return instance;
  }

  /**
   * Subscribes a handler for an event type.
   *
   * @param <T>      the event type
   * @param eventType the event class to subscribe to
   * @param handler the handler to invoke when the event is published
   */
  @SuppressWarnings("unchecked")
  public <T extends Message> void subscribe(
      final @NotNull Class<T> eventType,
      final @NotNull IEventHandler<? super T> handler) {
    this.handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
        .add((IEventHandler<? super Message>) handler);
  }

  /**
   * Unsubscribes a handler from an event type.
   *
   * @param <T>      the event type
   * @param eventType the event class to unsubscribe from
   * @param handler the handler to remove
   */
  public <T extends Message> void unsubscribe(
      final @NotNull Class<T> eventType,
      final @NotNull IEventHandler<T> handler) {
    final var list = this.handlers.get(eventType);

    if (list != null) {
      list.remove(handler);
    }
  }

  /**
   * Publishes an event to all subscribers.
   *
   * @param message the event to publish
   */
  public void publish(final @NotNull Message message) {
    final var list = this.handlers.get(message.getClass());

    if (list != null) {
      for (final var handler : list) {
        this.srv.submit(() -> handler.handle(message));
      }
    }
  }
}
