package de.yyuh.celery.api.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

  private record Subscription(
      @NotNull Subscriber subscriber,
      @NotNull IEventHandler<? super Message> handler,
      long registeredAt) {
  }

  private final Map<Class<?>, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
  private final ExecutorService srv = Executors.newVirtualThreadPerTaskExecutor();

  /**
   * Subscribes a handler for an event type.
   *
   * @param <T>        the event type
   * @param subscriber {@code Subscription} specific values
   * @param handler    the handler to invoke when the event is published
   */
  @SuppressWarnings("unchecked")
  public <T extends Message> Subscription subscribe(
      final @NotNull Subscriber subscriber,
      final @NotNull IEventHandler<? super T> handler) {
    final var eventType = (Class<T>) subscriber.getMessage().getClass();

    final var subscription = new Subscription(
        subscriber,
        (IEventHandler<? super Message>) handler,
        System.nanoTime());

    this.subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
        .add(subscription);

    return subscription;

  }

  /**
   * Subscribes a handler for an event type. Defaults to a permanent
   * {@code Subscription}
   *
   * @param <T>       the event type
   * @param eventType the event class to subscribe to
   * @param handler   the handler to invoke when the event is published
   */
  @SuppressWarnings("unchecked")
  public <T extends Message> Subscription subscribe(
      final @NotNull Class<T> eventType,
      final @NotNull IEventHandler<? super T> handler) {
    final var subscription = new Subscription(
        new Subscriber(null, null),
        (IEventHandler<? super Message>) handler,
        System.nanoTime());

    this.subscriptions.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
        .add(subscription);

    return subscription;
  }

  /**
   * Unsubscribes a handler from an event type.
   *
   * @param <T>       the event type
   * @param eventType the event class to unsubscribe from
   * @param handler   the handler to remove
   */
  public <T extends Message> void unsubscribe(
      final @NotNull Class<T> eventType,
      final @NotNull Subscription subscription) {
    final var list = this.subscriptions.get(eventType);

    if (list != null) {
      list.removeIf(sub -> sub.handler == subscription.handler);
    }
  }

  /**
   * Publishes an event to all subscribers.
   *
   * @param message the event to publish
   */
  public void publish(final @NotNull Message message) {
    final var list = this.subscriptions.get(message.getClass());

    if (list == null) {
      return;
    }

    final var now = System.nanoTime();
    final var it = list.iterator();

    while (it.hasNext()) {
      final var sub = it.next();

      if (isExpired(sub, now)) {
        list.remove(sub);
        continue;
      }

      sub.subscriber.setTimesCalled(sub.subscriber.getTimesCalled() + 1);
      this.srv.submit(() -> sub.handler.handle(message));
    }
  }

  private boolean isExpired(
      final @NotNull Subscription sub,
      final long now) {
    final var subscriber = sub.subscriber;
    final var expiry = subscriber.getExpiry();

    if (expiry.getTimeUnit() == null) {
      return subscriber.getTimesCalled() >= expiry.getExpireAfter();
    }

    if (expiry == null || expiry.getExpireAfter() <= 0) {
      return false;
    }

    final var elapsed = now - sub.registeredAt;
    final var unit = expiry.getTimeUnit() != null ? expiry.getTimeUnit() : TimeUnit.SECONDS;
    final var threshold = unit.toNanos(expiry.getExpireAfter());

    return elapsed >= threshold;
  }
}
